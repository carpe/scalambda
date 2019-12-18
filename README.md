# Scalambda
###### Tools for building Scala-based Lambda Functions

Scalambda is composed of two separate libraries. One is an SBT plugin that should help you with any DevOps tasks. The other is a traditional library that provides utilities for writing Scala-based Lambda Functions.

## Usage

Add the plugin to your project in the `project/plugins.sbt` file:

```scala
// project/plugins.sbt
resolvers += "Artifactory Realm" at "https://bin.carpe.io/artifactory/sbt-release"
addSbtPlugin("io.carpe" % "sbt-scalambda" % scalambdaVersion)
```

After adding the plugin to your project, add each Lambda Function as a subproject in your project's `build.sbt` file. There are two possible function types. 

Here is an example of how to add each of them:

```scala
// build.sbt

// a basic lambda function
lazy val example = project
  .enablePlugins(CarpeCorePlugin, ScalambdaPlugin)
  .settings(
    lambdaFunction(
      functionName = ???, // example: "Example"
      functionHandler = ???, // example: "io.carpe.example::handleFunctionName"
      functionRoleArn = ??? // example: "arn:aws:iam::120864075170:role/MyLambdaFunctionRole"
    )
  )

// a lambda function to be used through API Gateway
lazy val apiExample = project
  .enablePlugins(CarpeCorePlugin, ScalambdaPlugin)
  .settings(
    apiGatewayProxyLambda(
      functionName = ???, // example: "Example"
      functionHandler = ???, // example: "io.carpe.example::handleFunctionName"
      functionRoleArn = ??? // example: "arn:aws:iam::120864075170:role/MyLambdaFunctionRole"
    )

```