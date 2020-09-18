package io.carpe.scalambda.terraform.composing

import io.carpe.scalambda.conf.ScalambdaFunction.DefinedFunction
import io.carpe.scalambda.conf.function.WarmerConfig.NoOp
import io.carpe.scalambda.conf.function.{EnvironmentVariable, FunctionRoleSource, VpcConf, WarmerConfig}
import io.carpe.scalambda.conf.utils.StringUtils
import io.carpe.scalambda.terraform.ast.Definition
import io.carpe.scalambda.terraform.ast.Definition.{Output, Variable}
import io.carpe.scalambda.terraform.ast.props.TValue
import io.carpe.scalambda.terraform.ast.props.TValue.{TArray, TBool, TObject, TResourceRef, TString}
import io.carpe.scalambda.terraform.ast.providers.aws
import io.carpe.scalambda.terraform.ast.providers.aws.BillingTag
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.LambdaFunctionAlias
import io.carpe.scalambda.terraform.ast.providers.aws.lambda.resources.{LambdaFunction, LambdaLayerVersion, ProvisionedConcurrency}
import io.carpe.scalambda.terraform.ast.providers.aws.s3.{S3Bucket, S3BucketItem}

object LambdaComposer {

  def defineLambdaResources(
                             isXrayEnabled: Boolean,
                             projectName: String,
                             scalambdaFunctions: List[DefinedFunction],
                             version: String,
                             s3Bucket: S3Bucket,
                             projectBucketItem: S3BucketItem,
                             dependenciesBucketItem: S3BucketItem,
                             billingTags: Seq[BillingTag]
                           ): (
    Seq[LambdaFunction],
      Seq[LambdaFunctionAlias],
      LambdaLayerVersion,
      Seq[Definition],
      Seq[Variable[_]],
      Seq[Output[_]]
    ) = {
    // create a lambda layer that can be shared by all functions that contains the dependencies of said functions.
    // this will be used to speed up deployments
    val layerName = s"${StringUtils.toSnakeCase(projectName)}_assembled_dependencies"
    val lambdaDependenciesLayer = LambdaLayerVersion(layerName, dependenciesBucketItem)
    val warmerVariable = Variable[TBool](
      name = "enable_warmers",
      description = Some("If set to true, this will enable configured lambda warmers. Be wary of the extra cost this incurs."),
      defaultValue = Some(TBool(false))
    )

    // create resources for each of the lambda functions and the variables they require
    val (lambdaFunctions, lambdaAliases, lambdaWarmingResources, lambdaVariables, outputs) =
      scalambdaFunctions.foldRight(
        Seq.empty[LambdaFunction],
        Seq.empty[LambdaFunctionAlias],
        Seq.empty[Definition],
        Seq.empty[Variable[_]],
        Seq.empty[Output[_]]
      )(
        (function: DefinedFunction, resources) => {
          val (
            functionResources,
            functionAliases,
            functionWarmingResources,
            variables,
            outputs
            ) = resources


          /**
           * Define Terraform variables for function
           */

          val (subnetIdVariables, subnetIds) = composeSubnetIds(function.terraformLambdaResourceName, function.approximateFunctionName, function.vpcConfig)

          val (sgIdVariables, securityGroupIds) = composeSecurityGroupIds(function.terraformLambdaResourceName, function.approximateFunctionName, function.vpcConfig)

          val functionRoleVariables: Seq[Variable[TString]] = Seq(
            function.iamRole match {
              case fromVariable: FunctionRoleSource.RoleFromVariable.type =>
                Some(
                  Variable[TString](
                    fromVariable.variableName(function),
                    description = Some(
                      s"Arn for the IAM Role to be used by the ${function.approximateFunctionName} Lambda Function."
                    ),
                    defaultValue = None
                  )
                )
              case FunctionRoleSource.RoleFromArn(_) =>
                None
            }
          ).flatten

          val functionEnvVariables: Seq[Variable[TString]] = function
            .environmentVariables
            .flatMap(_ match {
              case EnvironmentVariable.StaticVariable(key, value) =>
                None
              case EnvironmentVariable.VariableFromTF(key, variableName) =>
                Some(variableName)
            })
            .map(
              variable =>
                Variable[TString](
                  variable,
                  description = Some("Injected as ENV variable into lambda functions"),
                  defaultValue = None
                )
            )

          val additionalBillingTagsVariable: Variable[TObject] = Variable[TObject](
            s"${function.terraformLambdaResourceName}_billing_tags",
            description = Some(
              s"Additional billing tags for the function."
            ),
            // set default to empty object
            defaultValue = Some(TObject())
          )


          /**
           * Define Terraform resources for function
           */

          val functionResource =
            LambdaFunction(function, subnetIds, securityGroupIds, version, s3Bucket, projectBucketItem, lambdaDependenciesLayer, isXrayEnabled, billingTags = billingTags, additionalBillingTagsVariable = additionalBillingTagsVariable.ref)

          val functionAlias = aws.lambda.resources.LambdaFunctionAliasResource(functionResource, version, "Managed by Scalambda. The name of this alias is the version of the code that this function is using. It is either a version of a commit SHA.")

          val (warmingVariables: Seq[Variable[TBool]], functionWarming: Seq[Definition]) = {
            function.warmerConfig match {
              case WarmerConfig.Invocation(json) =>
                (Seq(warmerVariable), WarmerComposer.composeWarmer(functionAlias, warmerVariable, json).definitions)
              case WarmerConfig.NoOp =>
                (Seq(warmerVariable), WarmerComposer.composeWarmer(functionAlias, warmerVariable, NoOp.json).definitions)
              case WarmerConfig.ProvisionedConcurrency(concurrency) =>
                (Seq(warmerVariable), Seq(ProvisionedConcurrency(functionAlias, warmerVariable, concurrency)))
              case WarmerConfig.Cold =>
                (Nil, Nil)
            }
          }

          val functionVariables = functionRoleVariables ++ functionEnvVariables ++ warmingVariables ++ subnetIdVariables ++ sgIdVariables

          /**
           * Define Terraform outputs for function
           */
          val functionOutputs = Seq(
            Output(
              name = s"${function.terraformLambdaResourceName}_arn",
              description = Some(s"Arn for the ${function.approximateFunctionName} function"),
              isSensitive = false,
              value = TResourceRef(functionResource, "arn")
            ),
            Output(
              name = s"${function.terraformLambdaResourceName}_name",
              description = Some(s"Final name for the ${function.approximateFunctionName} function"),
              isSensitive = false,
              value = TResourceRef(functionResource, "function_name")
            ),
            Output(
              name = s"${function.terraformLambdaResourceName}_version",
              description = Some(s"Latest version number for the ${function.approximateFunctionName} function"),
              isSensitive = false,
              value = TResourceRef(functionResource, "version")
            )
          )

          (
            functionResources :+ functionResource,
            functionAliases :+ functionAlias,
            functionWarmingResources ++ functionWarming,
            variables ++ functionVariables :+ additionalBillingTagsVariable,
            outputs ++ functionOutputs
          )
        }
      )

    (lambdaFunctions, lambdaAliases, lambdaDependenciesLayer, lambdaWarmingResources, lambdaVariables.distinct, outputs)
  }


  private def composeSubnetIds(terraformFunctionName: String, approximateFunctionName: String, vpcConfig: VpcConf): (List[Variable[TArray[TString]]], TValue) = vpcConfig match {
    case VpcConf.StaticVpcConf(subnetIds, securityGroupIds) =>
      val variables = List.empty

      variables -> TArray(subnetIds.map(TString): _*)
    case VpcConf.VpcFromTF =>
      val tf = Variable[TArray[TString]](
        s"${terraformFunctionName}_subnet_ids",
        description = Some(
          s"Ids of subnets to place the ${approximateFunctionName} Lambda Function in."
        ),
        defaultValue = None
      )

      List(tf) -> tf.ref
  }

  private def composeSecurityGroupIds(terraformFunctionName: String, approximateFunctionName: String, vpcConfig: VpcConf): (List[Variable[TArray[TString]]], TValue) = {
    vpcConfig match {
      case VpcConf.StaticVpcConf(subnetIds, securityGroupIds) =>
        val variables = List.empty

        variables -> TArray(securityGroupIds.map(TString): _*)
      case VpcConf.VpcFromTF =>
        val tf = Variable[TArray[TString]](
          s"${terraformFunctionName}_security_group_ids",
          description = Some(
            s"Ids of security groups to attach to the ${approximateFunctionName} Lambda Function."
          ),
          defaultValue = None
        )

        List(tf) -> tf.ref
    }
  }
}
