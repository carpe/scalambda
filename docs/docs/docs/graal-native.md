---
layout: docs
title: Using Graal Native
permalink: /docs/thegraaaaal/
---

## What and why?

If you're not yet familiar with GraalVM's [Native Image builder](https://www.graalvm.org/reference-manual/native-image/), it essentially allows you to turn your Java code into a native executable. This essentially allows you to **run your code without a JVM**, which allows for nearly instantaneous start times. It also allows for some pretty awesome optimizations like the ability to **initialize certain objects at compile time**. Both of these advantages by themselves are incredibly powerful tools that can allow you to **decimate your function's cold start times**.    

#### Disclaimers

GraalVM's Native Image builder isn't quite magic, and comes with some serious risks. The good news is, the problems with Graal Native tend to come up at compile time, rather than during your function's runtime. However, debugging/fixing these build issues with your native images can be incredibly costly. One of my favorite posts that I think perfectly demonstrates just how time-consuming and intensive debugging these issues can be is [this post](https://medium.com/graalvm/instant-netty-startup-using-graalvm-native-image-generation-ed6f14ff7692), which talks about getting Netty to work with Graal Native.

Imagine the amount of time that went into figuring out the issues that post talks about. Spoiler alert: it takes a ton of time. If your function is incredibly simple with few dependencies, then you might be able to get away with turning your function into a Native Image. Otherwise, try to look at [other ways of reducing your cold start times](https://www.reddit.com/r/scala/comments/hw6iic/sbt_plugin_for_quick_and_easy_aws_lambda_function/fz08cje?utm_source=share&utm_medium=web2x&context=3).

**TL;DR:** Don't use Graal Native if you aren't prepared to invest huge amounts of time debugging a ton of build issues. If you have a dedicated team at your company that solves these kinds of issues, be prepared for everyone on that team to start hating you and don't expect them to be able to get your function to actually work.

All that being said, you can always revert to the `Java11` or `Java8` runtime if you can't get your function to work in `GraalNative` mode. So if you've some time you want to kill, you can follow the guide below to give Graal Native a shot to see if it works with your function.

## Configuration

One quick note before you start, I highly recommend that you try deploying a `Java8` or `Java11` with Scalambda first, before jumping into the deep end. **You can do it in under 5 minutes** using our [g8 template](https://github.com/carpe/scalambda.g8/) and it will give you a perfect plan B if/when you run into issues with Graal Native.   

#### Prerequisites

Scalambda uses [SBT Native Packager](https://sbt-native-packager.readthedocs.io/en/latest/) under the hood. Before running `scalambdaTerraform`, you'll need to make sure you have installed all of the [requirements](https://sbt-native-packager.readthedocs.io/en/latest/formats/graalvm-native-image.html#requirements).  

#### build.sbt

Below is an example `build.sbt` file for a fully configured lambda function using a Native Image. 

```scala
lazy val nativegreeter = (project in file("."))
  .enablePlugins(ScalambdaPlugin)
  .settings(
    // this call enables Scalambda, and sets the class found at this path to be the handler
    scalambda("science.doing.nativegreeter.NativeGreeter", runtime = GraalNative, memory = 256)
  ).settings(
    // graal native image settings
    // Options used by `native-image` when building native image
    // https://www.graalvm.org/docs/reference-manual/native-image/
    graalVMNativeImageOptions ++= Seq(
      "--initialize-at-build-time", // Auto-packs dependent libs at build-time
      "--no-fallback", // Bakes-in run-time reflection (alternately: --auto-fallback, --force-fallback)
      "--no-server", // Won't be running `graalvm-native-image:packageBin` often, so one less thing to break
      "--static", // Forces statically-linked binary, requires libc installation. Comment this out if you're using OSX
      "--enable-url-protocols=http" // Enables http requests, which are required in order to communicate with the AWS Lambda Runtime API
      // "--enable-url-protocols=http,https" // Enables both http and https requests
    )
  )
```

You will almost certainly need to tweak the configuration a bit depending on the needs for your function. You will almost certainly need to tweak the settings above in order for your code to successfully build. Checkout the [full list of available settings](https://sbt-native-packager.readthedocs.io/en/latest/formats/graalvm-native-image.html#settings) in sbt-native-packager's documentation.

**Important Note:** Due to current limitations on how we assemble your native image, **each sub-project can only include one `GraalNative` Scalambda Function**.

## Implementation

In order to get your function to execute properly, **you only need to do two things**.

1. Your main class must be an `object`.
2. Your main class must extend either `io.carpe.scalambda.native.ScalambdaIO` or `io.carpe.scalambda.native.Scalambda`. 

Scalambda automatically injects the library that includes the `io.carpe.scalambda.native` package when you set your function's runtime to `GraalNative`. 

Other than these two things, you shouldn't need to change anything else at all from the usual code you'd use for a JVM-based Scalambda Function. Of course, you may want to tweak things later.

```scala
package science.doing.nativegreeter

import cats.effect.IO
import io.carpe.scalambda.native.ScalambdaIO

// NOTE: we are using the `native` ScalambdaIO, not the JVM based one.
object NativeGreeter extends ScalambdaIO[String, String] {

  override def run(input: String): IO[String] = IO {
    "Hello, " + input + "!"
  }

}
```

## Building and Deploying

Assuming you already have the prerequisites installed, you can try to deploy your new Graal Native Lambda function the same way as any other Scalambda Lambda Function. Just run `sbt scalambdaTerraform` to generate the terraform and then apply it. For further details, checkout [Deploying Functions](https://carpe.github.io/scalambda/docs/deploying-functions/) for a more in-depth explanation.