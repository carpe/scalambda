package io.carpe.scalambda.api.conf

import com.amazonaws.services.lambda.runtime.Context

case class ApiContext(lambdaContext: Context)