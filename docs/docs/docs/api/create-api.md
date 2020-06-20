---
layout: docs
title: Create an API
permalink: /docs/api/create-api/
---

## Defining your API

Two things will be needed in order to have Scalambda generate the terraform that connects your Lambda Functions to Api Gateway:
1. You must set the `apiName` setting key to the desired name of your API Gateway instance. 
1. You must define a `scalambdaEndpoint` for each endpoint in you want in your API. Each of these endpoints will become a new Lambda Function. 

Here is how you might define the classic Petstore API example:

```scala
// build.sbt

lazy val petstore = project
  .enablePlugins(ScalambdaPlugin)
  .settings(
    // set the name of your api (this name must be unique to all Api Gateway instances in your current AWS Region)
    apiName := ??? // example: "petstore-api"
  )
  .settings(
    // This lambda will handle all POST requests to "<my api domain>/pets"
    scalambdaEndpoint(
      functionClasspath = ???, // example: "io.carpe.example.CreatePet"
      apiConfig = post("/pets")
    )   

    // This lambda will handle all GET requests to "<my api domain>/pets"
    scalambdaEndpoint(
      functionClasspath = ???, // example: "io.carpe.example.GetPets"
      apiConfig = get("/pets")
    )
  
    // This lambda will handle all GET requests to "<my api domain>/pets/<some pet id>"
    scalambdaEndpoint(
      functionClasspath = ???, // example: "io.carpe.example.GetPet"
      apiConfig = get("/pets/{id}")
    )

    // This lambda will handle all PUT requests to "<my api domain>/pets/<some pet id>"
    scalambdaEndpoint(
      functionClasspath = ???, // example: "io.carpe.example.UpdatePet"
      apiConfig = put("/pets/{id}")
    )

    // This lambda will handle all DELETE requests to "<my api domain>/pets/<some pet id>"
    scalambdaEndpoint(
      functionClasspath = ???, // example: "io.carpe.example.DeletePet"
      apiConfig = delete("/pets/{id}")
    )
  )
```

## Defining each Endpoint 

When Api Gateway receives a request, it will invoke the configured Lambda Function with what AWS calls an "Api Gateway Proxy Request". They also expect your function to provide a response in the form of an "Api Gateway Proxy Response".

Scalambda provides both of these as traits that you can use in your Lambda Functions like so:

```scala
package io.carpe.example

import com.amazonaws.services.lambda.runtime.Context
import io.carpe.scalambda.Scalambda
import io.carpe.scalambda.request.APIGatewayProxyRequest
import io.carpe.scalambda.response.APIGatewayProxyResponse
import io.carpe.scalambda.response.ApiError.InputError
import cats.data.NonEmptyChain

class Greeter extends Scalambda[APIGatewayProxyRequest[String], APIGatewayProxyResponse[String]] {

  /**
   * Accept a request that provides someone's name in a JSON body.
   *
   * Response with a greeting for that given person.
   *
   * @param input from api gateway that represents the request
   * @param context lambda request context
   * @return
   */
  override def handleRequest(input: APIGatewayProxyRequest[String], context: Context): APIGatewayProxyResponse[String] = {
    val greetingResponse = for {
      // attempt to get the provided name from the input
      inputName <- input.body

      // use it to create a greeting
      greeting = s"Hello, ${inputName}!"
    } yield {
      // place the greeting inside a response object, along with any headers that you'd like
      // to supply. 
      APIGatewayProxyResponse.WithBody(
        statusCode = 200,
        headers = Map(
          "content-type" -> "application/json"
        ),
        body = greeting
      )
    }

    // return the result, or an error to Api Gateway
    greetingResponse.getOrElse({
      APIGatewayProxyResponse.WithError(
        // ApiError has a default encoder that will be used to inject errors into the 
        // response body as json. You can override this encoder if you'd like, it is an implicit
        // parameter for the APIGatewayProxyResponse.WithError's constructor 
        errors = NonEmptyChain(InputError("No input was provided")),        
        headers = Map(
          "content-type" -> "application/json"
        )
      )
    })
  }
}
```

As you can see, there really isn't too much of a difference between a Lambda Function that serves requests from Api Gateway and one that does not. The only thing that changes is the input to your Function.