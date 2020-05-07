# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.0.0] - 2020-05-01
## Added
- A lot more documentation and companion website is now live.
- New options for mapping ApiGateway instances to Custom Domain Names.
- New Terraform outputs: `rest_api_id`, `rest_api_deployment_id`, and `rest_api_stage_name`.

## Changed
- SBT version has been bumped to 1.3.10. This was done to allow for the usage of [sbt-microsites](https://47degrees.github.io/sbt-microsites/).
- Using `java8` or `java11` to gain access to the `ScalambdaRuntime` is deprecated. Use `Java8` or `Java11` instead. 
- Similar to the above update, but a breaking change:`apiDomain` has been replaced by `ApiDomain`.
- Many configuration values have been moved into the `ScalambdaKeys` trait.

## [2.0.0] - 2020-04-26
## Added
- Ability to add Billing Tags to created resources via `billingTags` setting key and `BillingTag` function.
- New Lambda Function Alias resources will be created to ensure that Api Gateway Deployments always point to function versions that exist. This fixes the issue that caused outages between Api Deployments.
- Api Gateway Deployments will be automatically managed by Terraform. No more `terraform taint` commands!
- Lambda Function versions will be exposed as terraform module outputs.  

## Changed
- Custom Domain Name terraform resource now points to the active stage, rather than the intermediate stage. This insures that X-Ray traces show up properly. 

## Removed
- `Vpc` value in `autoImport` scope has been replaced by `VpcConfigKeys` trait mixed into ScalambdaKeys  

## [1.0.1] - 2020-04-20
## Changed
- Updates to build process. No visible changes to published plugin or lib. 

## [1.0.0] - 2020-04-13 
- Initial Release
 
