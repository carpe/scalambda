---
layout: docs
title: Getting Started
permalink: docs/getting-started/
---

## New Project

The easiest way to get started with Scalambda is via the Giter template. Run the following to get started immediately:

```
sbt --supershell=false new carpe/scalambda.g8
```

Check it out at https://github.com/carpe/scalambda.g8

## Add to Existing Project

Add the plugin to your project in the `project/plugins.sbt` file:

```scala
addSbtPlugin("io.carpe" % "sbt-scalambda" % "2.0.0")
```