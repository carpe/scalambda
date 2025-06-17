
import scala.sys.process._
lazy val result: String =
  "aws codeartifact get-authorization-token --domain carpedata --domain-owner 758823009815 --region us-west-2 --query authorizationToken --output text".!!

resolvers += "carpedata/scala-internal" at "https://carpedata-758823009815.d.codeartifact.us-west-2.amazonaws.com/maven/scala-internal"

credentials += Credentials(
  "carpedata/scala-internal",
  "carpedata-758823009815.d.codeartifact.us-west-2.amazonaws.com",
  "aws",
  (sys.env.get("CODEARTIFACT_AUTH_TOKEN") match {
    case Some(value) if value.nonEmpty => value
    case None                          => result
  }).trim
)
