package io.carpe.scalambda.terraform.openapi

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.api.ApiGatewayConfig
import io.carpe.scalambda.conf.function.Method

case class ResourcePath(name: String, post: Option[ResourceMethod], get: Option[ResourceMethod], put: Option[ResourceMethod], delete: Option[ResourceMethod], options: Option[ResourceMethod]) {
  def addFunction(apiConfig: ApiGatewayConfig, function: ScalambdaFunction): ResourcePath = {

    apiConfig.method match {
      case Method.POST =>
        this.post.fold(this.copy(post = Some(ResourceMethod.fromLambda(function))))(conflicting => { throw new RuntimeException(s"Tried to add ${function.terraformLambdaResourceName}, but it conflicted with another method: ${conflicting}")})
      case Method.GET =>
        this.get.fold(this.copy(get = Some(ResourceMethod.fromLambda(function))))(conflicting => { throw new RuntimeException(s"Tried to add ${function.terraformLambdaResourceName}, but it conflicted with another method: ${conflicting}")})
      case Method.PUT =>
        this.put.fold(this.copy(put = Some(ResourceMethod.fromLambda(function))))(conflicting => { throw new RuntimeException(s"Tried to add ${function.terraformLambdaResourceName}, but it conflicted with another method: ${conflicting}")})
      case Method.DELETE =>
        this.delete.fold(this.copy(delete = Some(ResourceMethod.fromLambda(function))))(conflicting => { throw new RuntimeException(s"Tried to add ${function.terraformLambdaResourceName}, but it conflicted with another method: ${conflicting}")})
    }
  }
}

object ResourcePath {

  import io.circe._

  implicit val encoder: Encoder[ResourcePath] = (api: ResourcePath) => {
    val paths = List(
      api.post.map("post" -> ResourceMethod.encoder.apply(_)),
      api.get.map("get" -> ResourceMethod.encoder.apply(_)),
      api.put.map("put" -> ResourceMethod.encoder.apply(_)),
      api.delete.map("delete" -> ResourceMethod.encoder.apply(_)),
      api.options.map("options" -> ResourceMethod.encoder.apply(_))
    ).flatten

    Json.obj(paths: _*)
  }
}
