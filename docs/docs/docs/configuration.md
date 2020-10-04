---
layout: docs
title: Configuration
permalink: /docs/configuration/
---

## Defining Scalambdas

After importing the plugin into your project, you can then use the `scalambda` function to define Lambda Functions from your project's sources.

##### Example

```scala
// build.sbt

// a basic lambda function using default settings
lazy val example = project
  .enablePlugins(ScalambdaPlugin)
  .settings(
    scalambda(
      functionClasspath = ??? // example: "io.carpe.example.ExampleFunction"
    )
  )

// you can also create multiple functions that share the same source
lazy val multipleFunctions = project
  .enablePlugins(ScalambdaPlugin)
  .settings(
    // first function
    scalambda(
      functionClasspath = ??? // example: "io.carpe.example.ExampleFunction"
    )
    // second function
    scalambda(
      functionClasspath = ??? // example: "io.carpe.example.ExampleFunction"
    )
  )

```

The first time the `scalambda` function is ran within a project, sbt will automatically inject in the `scalambda-core` and `scalambda-testing` libraries. These libraries provide you with a traits and helpers to make developing and testing lambda functions quick and easy.

## Settings

Once you have enabled the Scalambda plugin, you'll be able to set some settings that are shared by all functions within the current project. That being said, the bulk of the configuration options are configured by modifying the parameters to each `scalambda` function invocation.

#### Project Settings

This is the full list of settings that are shared by all your functions.

| Setting Key                     | Type            | Description                                             | Default Value                  |
| ------------------------------- | --------------- | ------------------------------------------------------- | ------------------------------:|
| s3BucketName                    | String          | Prefix for S3 bucket name to store binaries in          | `sbt.Keys.name`                |
| billingTags                     | Seq[BillingTag] | AWS Billing Tags to apply to all terraformed resources. You can also provide billing tags via a terraform variable in the generated module. See below for details. | `Nil` |
| scalambdaTerraformPath          | File            | Path to where terraform should be written to            | `sbt.Keys.target / "terraform"` |
| scalambdaDependenciesMergeStrat | MergeStrategy   | `sbt-assembly` MergeStrategy for your dependencies jar  |  [Check it out](https://github.com/carpe/scalambda/blob/develop/plugin/src/main/scala/io/carpe/scalambda/assembly/AssemblySettings.scala#L39-L46) |
| enableXray                      | Boolean         | If set to true, injects AWS Xray SDK into your Lambda and enables Passthrough mode | `false` | 
| apiName                         | String          | Name for Api Gateway instance                            | `sbt.Keys.name`                |
| domainName                      | ApiDomain       | Domain name for Api Gateway                              | - |

#### Lambda Settings

Each `scalambda` function accepts a wide range of parameters. Although, the only required parameter is the `functionClasspath`.

| Parameter                       | Type                     | Description                                             | Default Value                  |
| ------------------------------- | ------------------------ | ------------------------------------------------------- | ------------------------------:|
| functionClasspath               | String                   | path to the class that contains the handler for your lambda function   | -               |
| functionNaming                  | FunctionNaming           | controls how your lambda function is named  | `WorkspaceBased` |
| iamRoleSource                   | FunctionRoleSource       | controls how your lambda function receives it's IAM role. Options are `RoleFromVariable` or `RoleFromArn` | `RoleFromVariable` |
| memory                          | Int                      | amount of memory for your function to use (in MBs)  | 1536 |
| runtime                         | ScalambdaRuntime         | runtime for your function to use. Options are `Java8`, `Java11`, or `GraalNative` | `Java8` | 
| concurrencyLimit                | Int                      | maximum number of concurrent instances of your Function | - |
| warmWith                        | WarmerConfig             | controls how your lambda function will be kept "warm" | `WarmerConfig.Cold` |
| vpcConfig                       | VpcConfig                | use this setting if you need to run your Lambda Function inside a VPC. Options are `StaticVpcConfig` and `VpcFromTF` | `VpcConf.withoutVpc` |
| environmentVariables            | Seq[EnvironmentVariable] | use this to inject ENV variables into your Lambda Function. Options are `StaticVariable` and `VariableFromTF` | Nil |

Note: If you use the GraalNative runtime, please make sure to read [the documentation page](https://carpe.github.io/scalambda/docs/thegraaaaal/) for additional information (and a list of disclaimers a mile long). 

#### Terraform Module Settings

When/if you run `scalambdaTerraform`, several variables will be generated in the outputted module.

| Parameter                       | Type                     | Description                                             | Default Value                  |
| ------------------------------- | ------------------------ | ------------------------------------------------------- | ------------------------------:|
| <your_function_name>_billing_tags | map                      | Billing tags for the function. These will be merged with the billing tags provided via the plugin `billingTags` setting.  | {} |
| s3_billing_tags                 | map                      | Billing tags for the S3 bucket. These will be merged with the billing tags provided via the plugin `billingTags` setting. | {} |

## Tasks

The following tasks are also available to each project that has enabled the plugin.

| Setting Key                     | Description                                                              |
| ------------------------------- | ------------------------------------------------------------------------ |     
| scalambdaTerraform              | Produces a Terraform module from the project's scalambda configuration   |
| scalambdaPackage                | Create jar (without dependencies) for your Lambda Function(s)            |
| scalambdaPackageDependencies    | Create a jar containing all the dependencies for your Lambda Function(s) |