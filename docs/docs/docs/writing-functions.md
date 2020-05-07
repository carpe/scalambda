---
layout: docs
title: Writing Functions
permalink: /docs/writing-functions/
---

## Writing Lambdas with Scalambda

Scalambda automatically adds `scalambda-core` as a dependency to your project when you enable it. It is a library designed to help you get started coding lambda functions as quickly as possible.  

The `scalambda-core` library currently comes with two traits for defining Lambda functions. Both use circe for encoding and decoding of your function's input and output.

- `io.carpe.scalambda.Scalambda` gives you the most freedom and control for your functions.
- `io.carpe.scalambda.effect.ScalambdaIO` allows you to write functions using cats-effect's powerful IO. 


 ```scala
package io.carpe

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.Scalambda
 
class HelloWorld extends Scalambda[String, String] {
 
  override def handleRequest(input: String, context: Context): String = {
    "Hello, ${input}!"
  }
}
 ```
 
## Inputs and Outputs

You can use any type you want for the input and output of your function, so long as you've defined a [Encoder and Decoder](https://circe.github.io/circe/codecs/custom-codecs.html) for that given type.

You have a few options for how you'd like to define these encoders and decoders. At Carpe Data, we like to use [Semi-Automatic Derivation](https://meta.plasm.us/posts/2016/01/14/configuring-generic-derivation/). 

By placing these encoders and decoders into companion objects within your Input and Output classes, you can guarantee that they will be in scope for your functions.

###### Example

Here is a custom case class we want to use as the output and/or input to our Lambda Function.

```scala
package io.carpe.views

case class Car(make: String, model: String)

object Car {
  import io.circe.generic.extras.semiauto._
  import io.circe.{Decoder, Encoder}

  implicit val decoder: Decoder[Car] = deriveConfiguredDecoder[Car]
  implicit val encoder: Encoder[Car] = deriveConfiguredEncoder[Car]
}
```

This function accepts a Car as an argument, then automatically turns that Car into a nice sports car. A dumb example, but hopefully it gives you the general idea.

Since it imports `Car`, the implicit decoder and encoder for a Car will be made visible to Scalambda. This allows Scalambda to use it to decode the input and encode the output for you, 

```scala
package io.carpe

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.Scalambda
import io.carpe.views.Car
 
class UpgradeCar extends Scalambda[Car, Car] {
 
  override def handleRequest(input: Car, context: Context): Car = {
    // turn the input into a 911, then return it
    input.copy(make = "Porsche", model = "911 GT3")
  }
}
```