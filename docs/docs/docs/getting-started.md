---
layout: docs
title: Getting Started
permalink: /docs/
---

# Scalambda

Deploying Lambda functions is time-consuming, so we built Scalambda to make it quick and easy. Using Scalambda, you can enable developers to easily build and deploy their own Lambda Functions (and/or ApiGateway instances) with little to no effort or knowledge of AWS required.

## Create a new Project

The easiest way to get started with Scalambda is via the Giter template. Run the following to get started immediately:

```
sbt --supershell=false new carpe/scalambda.g8
```

Check it out at https://github.com/carpe/scalambda.g8

## Add to an existing Project

Add the plugin to your project in the `project/plugins.sbt` file:

```scala
addSbtPlugin("io.carpe" % "sbt-scalambda" % "3.0.0")
```

## Motivation

Our motivations for Scalambda were:
- Make it so anybody can deploy an API to AWS with two or less steps
- Simplify configuration and optimization of Lambda Functions
- Speed up the time to market of new projects by providing as many sensible defaults as we could
- Give developers as much freedom as possible so they can be free to experiment with new ideas

Scalambda started as an internal-only project over a year ago. Over the course of its lifetime it has received a TON of feedback and refinement from several of our teams and friends. Thanks to their efforts, we think we've managed to land on a solution that is an incredibly powerful tool.

## What is it?

Scalambda itself is composed of three separate libraries. Each of them can be used independently depending on your project's use case and your team's toolchain. 

- `sbt-scalambda` An SBT plugin that should help you to deploy your lambdas, managing libraries, logging and much more.

- `scalambda-core` A traditional library that provides utilities for writing Scala-based Lambda Functions

- `scalambda-testing` A set of test helpers for testing Lambda Functions

## (Short-term) Roadmap

Top priority is continue to create more documentation as well as add some example projects to help people get their Lambda Functions deployed even quicker. 

In the meantime, if you have any questions, please don't hesitate to [Open an Issue](https://github.com/carpe/scalambda/issues/new/choose) on our Github repo!  