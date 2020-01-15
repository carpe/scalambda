package io.carpe.scalambda.aws

import java.time.Instant

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.AWSLambdaClientBuilder
import com.amazonaws.services.lambda.model._
import com.gilt.aws.lambda._
import com.typesafe.scalalogging.LazyLogging
import io.carpe.scalambda.conf.QualifiedLambdaArn

import scala.util.{Failure, Try}

trait Lambda extends LazyLogging {
  protected def createFunction(req: CreateFunctionRequest): Try[CreateFunctionResult]
  protected def updateFunctionCode(req: UpdateFunctionCodeRequest): Try[UpdateFunctionCodeResult]
  protected def getFunctionConfiguration(req: GetFunctionConfigurationRequest): Try[GetFunctionConfigurationResult]
  protected def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest): Try[UpdateFunctionConfigurationResult]
  protected def tagResource(req: TagResourceRequest): Try[TagResourceResult]
  protected def publishVersion(request: PublishVersionRequest): Try[PublishVersionResult]
  protected def createAlias(request: CreateAliasRequest): Try[CreateAliasResult]
  protected def updateAlias(request: UpdateAliasRequest): Try[UpdateAliasResult]

  import scala.collection.JavaConverters._

  def publishVersion(name: String, revisionId: String, version: String)
  : Try[PublishVersionResult] = {
    val request = new PublishVersionRequest()
      .withFunctionName(name)
      .withRevisionId(revisionId)
      .withDescription(version)
    publishVersion(request)
  }

  def updateLambdaWithFunctionCodeRequest(updateFunctionCodeRequest: UpdateFunctionCodeRequest, version: String): Try[UpdateFunctionCodeResult] = {
    println(s"Updating lambda code ${updateFunctionCodeRequest.getFunctionName}")
    for {
      updateResult <- updateFunctionCode(updateFunctionCodeRequest)
      _ = println(s"Updated lambda code ${updateResult.getFunctionArn}")
      _ <- publishVersion(name = updateResult.getFunctionName, revisionId = updateResult.getRevisionId, version = version)
    } yield {
      updateResult
    }
  }

  def tagLambda(functionArn: String, version: String) = {
    val tags = Map(
      "deploy.code.version" -> version,
      "deploy.timestamp" -> Instant.now.toString
    )

    val tagResourceReq = new TagResourceRequest()
      .withResource(functionArn)
      .withTags(tags.asJava)

    tagResource(tagResourceReq)
  }

  def getLambdaConfig(functionName: LambdaName): Try[Option[GetFunctionConfigurationResult]] = {
    val request = new GetFunctionConfigurationRequest()
      .withFunctionName(functionName.value)

    getFunctionConfiguration(request)
      .map(Option.apply)
      .recover {
        case _: ResourceNotFoundException => None
      }
  }

  def updateLambdaConfig(functionName: LambdaName,
                         handlerName: HandlerName,
                         roleName: RoleARN,
                         timeout:  Option[Timeout],
                         memory: Option[Memory],
                         deadLetterName: Option[DeadLetterARN],
                         vpcConfig: Option[VpcConfig],
                         environment: Environment,
                         version: String): Try[UpdateFunctionConfigurationResult] = {

    var request = new UpdateFunctionConfigurationRequest()
      .withFunctionName(functionName.value)
      .withHandler(handlerName.value)
      .withRole(roleName.value)
      .withRuntime(com.amazonaws.services.lambda.model.Runtime.Java8)
      .withEnvironment(environment)

    request = timeout.fold(request)(t => request.withTimeout(t.value))
    request = memory.fold(request)(m => request.withMemorySize(m.value))
    request = vpcConfig.fold(request)(request.withVpcConfig)
    request = deadLetterName.fold(request)(d => request.withDeadLetterConfig(new DeadLetterConfig().withTargetArn(d.value)))

    for {
      updateResult <- updateFunctionConfiguration(request)
      _ = println(s"Updated lambda config ${updateResult.getFunctionArn}")
      _ <- publishVersion(name = updateResult.getFunctionName, revisionId = updateResult.getRevisionId, version = version)
    } yield {
      updateResult
    }
  }

  def createLambda(functionName: LambdaName,
                   handlerName: HandlerName,
                   roleName: RoleARN,
                   timeout:  Option[Timeout],
                   memory: Option[Memory],
                   deadLetterName: Option[DeadLetterARN],
                   vpcConfig: Option[VpcConfig],
                   functionCode: FunctionCode,
                   environment: Environment,
                   version: String): Try[CreateFunctionResult] = {

    var request = new CreateFunctionRequest()
      .withFunctionName(functionName.value)
      .withHandler(handlerName.value)
      .withRole(roleName.value)
      .withRuntime(com.amazonaws.services.lambda.model.Runtime.Java8)
      .withEnvironment(environment)
      .withCode(functionCode)
    request = timeout.fold(request)(t => request.withTimeout(t.value))
    request = memory.fold(request)(m => request.withMemorySize(m.value))
    request = vpcConfig.fold(request)(request.withVpcConfig)
    request = deadLetterName.fold(request)(n => request.withDeadLetterConfig(new DeadLetterConfig().withTargetArn(n.value)))

    for {
      createResult <- createFunction(request)
      _ <- publishVersion(name = createResult.getFunctionName, revisionId = createResult.getRevisionId, version = version)
    } yield {
      println(s"Create lambda ${createResult.getFunctionArn}")
      createResult
    }
  }

  def migrateAlias(alias: String, qualifiedArn: QualifiedLambdaArn): Try[QualifiedLambdaArn] = {
    try {
      createAlias(
        new CreateAliasRequest()
          .withName(alias)
          .withFunctionName(qualifiedArn.arn)
          .withFunctionVersion(qualifiedArn.qualifier)
          .withDescription({
            alias match {
              case "production" => "Production ready version of this Lambda Function. This alias is managed by Scalambda's release process."
              case _ => "This alias is managed by Scalambda."
            }})

      // switch the qualified lambda arn's qualifier to the alias instead of the version
      ).map(_ => qualifiedArn.copy(qualifier = alias))
    } catch {
      case aliasAlreadyExists: ResourceConflictException =>
        logger.info(s"Function alias already existed. Migrating the existing alias to the newly deployed ${qualifiedArn.arn}")
        updateAlias(
          new UpdateAliasRequest()
            .withName(alias)
            .withFunctionName(qualifiedArn.arn)
            .withFunctionVersion(qualifiedArn.qualifier)
            .withDescription({
              alias match {
                case "production" => "Production ready version of this Lambda Function. This alias is managed by Scalambda's release process."
                case _ => "This alias is managed by Scalambda."
              }})

        // switch the qualified lambda arn's qualifier to the alias instead of the version
        ).map(_ => qualifiedArn.copy(qualifier = alias))
      case e: Exception =>
        // handle failed
        logger.error(s"Failed to create alias for ${qualifiedArn.arn}.", e)
        Failure(e)
    }
  }
}

object Lambda {
  def instance(region: Regions): Lambda = {
    val auth = new DefaultAWSCredentialsProviderChain()
    val client = AWSLambdaClientBuilder.standard()
      .withCredentials(auth)
      .withRegion(region)
      .build

    new Lambda {
      def createFunction(req: CreateFunctionRequest) = Try(client.createFunction(req))
      def updateFunctionCode(req: UpdateFunctionCodeRequest) = Try(client.updateFunctionCode(req))
      def getFunctionConfiguration(req: GetFunctionConfigurationRequest) = Try(client.getFunctionConfiguration(req))
      def updateFunctionConfiguration(req: UpdateFunctionConfigurationRequest) = Try(client.updateFunctionConfiguration(req))
      def tagResource(req: TagResourceRequest) = Try(client.tagResource(req))
      def publishVersion(request: PublishVersionRequest) = Try(client.publishVersion(request))
      def createAlias(request: CreateAliasRequest) = Try(client.createAlias(request))
      def updateAlias(request: UpdateAliasRequest) = Try(client.updateAlias(request))
    }
  }
}
