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
  .enablePlugins(CarpeCorePlugin, ScalambdaPlugin)
  .settings(
    lambdaFunction(
      functionClasspath = ??? // example: "io.carpe.example.ExampleFunction"
    )
  )

// a lambda function to be used through API Gateway
lazy val apiExample = project
  .enablePlugins(CarpeCorePlugin, ScalambdaPlugin)
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
  .enablePlugins(CarpeCorePlugin, ScalambdaPlugin)
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
  .enablePlugins(CarpeCorePlugin, ScalambdaPlugin)
  .settings(
    lambdaFunction(
      functionClasspath = ??? // example: "io.carpe.example.ExampleFunction"
    )
  )

```

This would change the name of the function that is deployed from `ExampleFunction` to `MyExampleApiExampleFunction`. This is helpful when you have many functions and want to logically group them, it also helps keep function names descriptive!

## Lib Usage

Assuming you enabled the `ScalambdaPlugin` in your project, the `scalambda` library will automatically be injected into your project as a dependency.  

To use this library, make the class that your `functionClasspath` property points to extend either `Scalambda` or `ApiScalambda`. Here is an example:

```scala
import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.Scalambda

class HelloWorld extends Scalambda[String, String] {

  override def handleRequest(input: String, context: Context): String = {
    "Hello, ${input}!";
  }
}
```
 
More documentation is coming in the future. In the meantime, review the java docs or reach out to one of the maintainers of this library if you have questions. 

## Deploying your Lambda Function

Once you have written the code for your function, you can run `sbt configureLambda` to create the lambda function. 

Any future updates after this can be done by running `sbt deployLambda`. This will create a new version of the Lambda Function, using the current `version` property set by your project's sbt configuration.
