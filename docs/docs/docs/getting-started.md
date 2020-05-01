---
layout: docs
title: Getting Started
permalink: /docs/
---

# Scalambda
###### Toolkit for building/deploying Lambda Functions with Terraform

Deploying Lambda functions is time-consuming, so we built Scalambda to make it quick and easy. Using scalambda, you can enable developers to easily build and deploy their own Lambda Functions (and/or ApiGateway instances) with little to no effort or knowledge of AWS required.

Scalambda is composed of three separate libraries. One is an SBT plugin that should help you with any DevOps tasks. The other is a traditional library that provides utilities for writing Scala-based Lambda Functions.

## Create a new Project

The easiest way to get started with Scalambda is via the Giter template. Run the following to get started immediately:

```
sbt --supershell=false new carpe/scalambda.g8
```

Check it out at https://github.com/carpe/scalambda.g8

## Add to an existing Project

Add the plugin to your project in the `project/plugins.sbt` file:

```scala
addSbtPlugin("io.carpe" % "sbt-scalambda" % "2.0.0")
```