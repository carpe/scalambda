# Scalambda
###### Tools for building Scala-based Lambda Functions

Scalambda is composed of two separate libraries. One is an SBT plugin that should help you with any DevOps tasks. The other is a traditional library that provides utilities for writing Scala-based Lambda Functions.

## Plugin Usage

Add the plugin to your project in the `project/plugins.sbt` file:

```scala
// Carpe's own Artifactory
resolvers += "Artifactory Realm" at "https://bin.carpe.io/artifactory/sbt-release"
resolvers += "Artifactory Realm Snapshots" at "https://bin.carpe.io/artifactory/sbt-dev"
import scala.tools.nsc.Properties
credentials += Credentials(new File(Properties.envOrElse("JENKINS_HOME", Properties.envOrElse("HOME", "")) + "/.sbt/.credentials"))

// CarpeData's own sbt plugin for jumpstarting Lambda Function development
addSbtPlugin("io.carpe" % "sbt-scalambda" % "0.6.0")
```

After adding the plugin to your project, add each Lambda Function as a subproject in your project's `build.sbt` file. There are two possible function types. 

Here is an example of how to add each of them:

```scala
// build.sbt

// this is the ARN for the IAM role that your Lambda function will assume
scalambdaRoleArn := "arn:aws:iam::120864075170:role/MyLambdaFunctionRole"


// a basic lambda function
lazy val example = project
  .enablePlugins(ScalambdaPlugin)
  .settings(
    lambdaFunction(
      functionClasspath = ??? // example: "io.carpe.example.ExampleFunction"
    )
  )

// a lambda function to be used through API Gateway
lazy val apiExample = project
  .enablePlugins(ScalambdaPlugin)
  .settings(
    apiGatewayProxyLambda(
      functionClasspath = ??? // example: "io.carpe.example.ExampleFunction"
    )
  )

```

Both of these configurations will set default arguments for the [sbt-aws-lambda](https://github.com/saksdirect/sbt-aws-lambda) plugin. You can override any of the attributes this plugin exposes. A good example of when you might want to do this is if you want to deploy your Lambda Function into an AWS VPC, in which case. Here is an example:

```scala
// build.sbt

// this is the ARN for the IAM role that your Lambda function will assume
scalambdaRoleArn := "arn:aws:iam::120864075170:role/MyLambdaFunctionRole"

// a lambda function to be used through API Gateway
lazy val apiExample = project
  .enablePlugins(ScalambdaPlugin)
  .settings(
    apiGatewayProxyLambda(
      functionClasspath = ??? // example: "io.carpe.example.ExampleFunction"
    )
  ).settings(
    // these settings will place this lambda within the private subnet of our own ngvpc
    vpcConfigSubnetIds := Some("subnet-08cbd1981c909822b,subnet-066982c21edde753b,subnet-04017edfe468d1f3f"),
    vpcConfigSecurityGroupIds := Some("sg-0eb004aaa50425d07")

    // you can also override other properties here if you'd like, like the s3 bucket where the lambda will be stored
    // checkout the documentation on the sbt-aws-lambda lambda plugin (link above) to see more options
    s3Bucket := Some("another-bucket-besides-carpe-lambdas")
  )

```

This plugin also allows you to set a prefix that will be prepended to the name of any Lambda Functions this plugin creates which you can set in your project's global settings like:

```scala
// build.sbt
ThisBuild / scalambdaRoleArn := "arn:aws:iam::120864075170:role/MyLambdaFunctionRole"
ThisBuild / functionNamePrefix := Some("MyExampleApi")

// a basic lambda function
lazy val example = project
  .enablePlugins(ScalambdaPlugin)
  .settings(
    lambdaFunction(
      functionClasspath = ??? // example: "io.carpe.example.ExampleFunction"
    )
  )

```

This would change the name of the function that is deployed from `ExampleFunction` to `MyExampleApiExampleFunction`. This is helpful when you have many functions and want to logically group them, it also helps keep function names descriptive!

#### Full Settings List

| sbt setting   | Environment variable      |  Description |
|:----------|:----------|:---------------|
| scalambdaAlias | SCALAMBDA_ALIAS | Function Alias to attach to newly deployed Lambda Function versions |
| scalambdaRoleArn | - | ARN for AWS Role to use for lambda functions |
| functionNamePrefix | - | Prefix to prepend onto the names of any AWS Functions defined and deployed via Scalambda |
| s3Bucket |  AWS_LAMBDA_BUCKET_ID | The name of an S3 bucket where the lambda code will be stored |
| s3KeyPrefix | AWS_LAMBDA_S3_KEY_PREFIX | The prefix to the S3 key where the jar will be uploaded |
| lambdaName |    AWS_LAMBDA_NAME   |   The name to use for this AWS Lambda function. Defaults to the project name |
| handlerName | AWS_LAMBDA_HANDLER_NAME |    Java class name and method to be executed, e.g. `com.gilt.example.Lambda::myMethod` |
| region |  AWS_REGION | The name of the AWS region to connect to. Defaults to `us-east-1` |
| awsLambdaTimeout | AWS_LAMBDA_TIMEOUT | The Lambda timeout in seconds (1-900). Defaults to AWS default. |
| awsLambdaMemory | AWS_LAMBDA_MEMORY | The amount of memory in MB for the Lambda function (128-1536, multiple of 64). Defaults to AWS default. |
| lambdaHandlers |              | Sequence of Lambda names to handler functions (for multiple lambda methods per project). Overrides `lambdaName` and `handlerName` if present. | 
| deployMethod | AWS_LAMBDA_DEPLOY_METHOD | The preferred method for uploading the jar, either `S3` for uploading to AWS S3 or `DIRECT` for direct upload to AWS Lambda |
| deadLetterArn | AWS_LAMBDA_DEAD_LETTER_ARN | The [ARN](http://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html "AWS ARN documentation") of the Lambda function's dead letter SQS queue or SNS topic, to receive unprocessed messages |
| vpcConfigSubnetIds | AWS_LAMBDA_VPC_CONFIG_SUBNET_IDS | Comma separated list of subnet IDs for the VPC |
| vpcConfigSecurityGroupIds | AWS_LAMBDA_VPC_CONFIG_SECURITY_GROUP_IDS | Comma separated list of security group IDs for the VPC |
| environment  |                | Seq[(String, String)] of environment variables to set in the lambda function |

## Lib Usage

Assuming you enabled the `ScalambdaPlugin` in your project, the `scalambda` library will automatically be injected into your project as a dependency.  

To use this library, make the class that your `functionClasspath` property points to extend either `Scalambda` or `ApiScalambda`. Here is an example:

```scala
import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.Scalambda

class HelloWorld extends Scalambda[String, String] {

  override def handleRequest(input: String, context: Context): String = {
    "Hello, ${input}!"
  }
}
```
 
More documentation is coming in the future. In the meantime, review the java docs or reach out to one of the maintainers of this library if you have questions. 

## Deploying your Lambda Function

**TL;DR:** Run `sbt configureLambda` to create/change settings for your Lambda Function. If you wanna update your Lambda, run `sbt scalambdaPublish`.

#### Create/Configure Lambda

If the function(s) you are writing have never been deployed before, you can run `sbt configureLambda` to create the Lambda Function(s) with whatever settings you have set. If you ever change the settings of your Lambda function such as it's memory, provisioned concurrency, etc. you will need to run this command again.

#### Subsequent Deployments  

Anytime you'd like to deploy your Lambda Function, you can use `sbt scalambdaPublish`. This will use the AWS SDKs to build a jar for your application, then upload that jar as the code for a new version of a pre-existing Lambda Function.

If you are deploying your Lambda function into a production environment, it's advised that you use a Lambda Alias in order to manage usage of your Lambda Function and insulate your users from new deployments. You can set one via the `scalambdaAlias` property.

Protip: You can set your `scalambdaAlias` for the entire project by adding `ThisBuild / scalambdaAlias := Some("your alias name here")` to the root of your `build.sbt` file.