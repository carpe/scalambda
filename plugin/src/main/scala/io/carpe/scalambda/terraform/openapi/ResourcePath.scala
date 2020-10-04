package io.carpe.scalambda.terraform.openapi

import io.carpe.scalambda.conf.ScalambdaFunction
import io.carpe.scalambda.conf.api.ApiGatewayEndpoint
import io.carpe.scalambda.conf.function.Method

case class ResourcePath(name: String, post: Option[ResourceMethod], get: Option[ResourceMethod], put: Option[ResourceMethod], patch: Option[ResourceMethod], delete: Option[ResourceMethod], head: Option[ResourceMethod], options: Option[ResourceMethod]) {
  def addFunction(endpoint: ApiGatewayEndpoint, function: ScalambdaFunction): ResourcePath = {

    endpoint.method match {
      case Method.POST =>
        this.post.fold(this.copy(post = Some(ResourceMethod.fromLambda(endpoint, function))))(conflicting => { throw new RuntimeException(s"Tried to add ${function.terraformLambdaResourceName}, but it conflicted with another method: ${conflicting}")})
      case Method.GET =>
        this.get.fold(this.copy(get = Some(ResourceMethod.fromLambda(endpoint, function))))(conflicting => { throw new RuntimeException(s"Tried to add ${function.terraformLambdaResourceName}, but it conflicted with another method: ${conflicting}")})
      case Method.PUT =>
        this.put.fold(this.copy(put = Some(ResourceMethod.fromLambda(endpoint, function))))(conflicting => { throw new RuntimeException(s"Tried to add ${function.terraformLambdaResourceName}, but it conflicted with another method: ${conflicting}")})
      case Method.PATCH =>
        this.patch.fold(this.copy(patch = Some(ResourceMethod.fromLambda(endpoint, function))))(conflicting => { throw new RuntimeException(s"Tried to add ${function.terraformLambdaResourceName}, but it conflicted with another method: ${conflicting}")})
      case Method.DELETE =>
        this.delete.fold(this.copy(delete = Some(ResourceMethod.fromLambda(endpoint, function))))(conflicting => { throw new RuntimeException(s"Tried to add ${function.terraformLambdaResourceName}, but it conflicted with another method: ${conflicting}")})
      case Method.HEAD =>
        this.head.fold(this.copy(head = Some(ResourceMethod.fromLambda(endpoint, function))))(conflicting => { throw new RuntimeException(s"Tried to add ${function.terraformLambdaResourceName}, but it conflicted with another method: ${conflicting}")})
      case Method.OPTIONS =>
        this.options.fold(this.copy(options = Some(ResourceMethod.fromLambda(endpoint, function))))(conflicting => { throw new RuntimeException(s"Tried to add ${function.terraformLambdaResourceName}, but it conflicted with another method: ${conflicting}")})
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
      api.patch.map("patch" -> ResourceMethod.encoder.apply(_)),
      api.delete.map("delete" -> ResourceMethod.encoder.apply(_)),
      api.head.map("head" -> ResourceMethod.encoder.apply(_)),
      api.options.map("options" -> ResourceMethod.encoder.apply(_))
    ).flatten

    Json.obj(paths: _*)
  }
}
