---
layout: docs
title: Create an API
permalink: /docs/api/create-api/
---

## Defining your API 

Here is how you might define the classic Petstore API example:

```scala
// build.sbt

lazy val petstore = project
  .enablePlugins(ScalambdaPlugin)
  .settings({
    // save the lambda function to a value so you can re-use it across multiple endpoints 
    lazy val petsHandler = Function(
      functionClasspath = ??? // example: "io.carpe.example.CreatePet"
    )
    
    // this function allows us name our api and map the lambda function above to various endpoints
    apiGatewayDefinition(apiGatewayInstanceName = "petstore-api-${terraform.workspace}")(
      // This sends all POST requests to "<my api domain>/pets" to our lambda function
      POST("/pets") -> petsHandler,
      // This sends all GET requests to "<my api domain>/pets" to our lambda function
      GET("/pets") -> petsHandler,
      // This sends all GET requests to "<my api domain>/pets/<some pet id>" to our lambda function
      // (it also makes "id" available as a path parameter, inside the pathParameters field on the request)
      GET("/pets/{id}") -> petsHandler
    )
  })
```

As you can see in the above example, we can map the same function to multiple endpoints. This helps keep cold start times down by allowing your functions to be re-used more frequently. You can just as easily define a lambda function for each endpoint if you'd prefer.

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