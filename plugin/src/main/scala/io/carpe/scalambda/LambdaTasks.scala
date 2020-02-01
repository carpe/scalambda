package io.carpe.scalambda

import java.io.File

import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest
import com.gilt.aws.lambda.{FileOps, HandlerName, LambdaName, S3BucketId}
import io.carpe.scalambda.aws.{Lambda, S3}
import io.carpe.scalambda.conf.QualifiedLambdaArn

import scala.util.{Failure, Success, Try}

object LambdaTasks {

  import cats.implicits._

  /**
   * Publish a Lambda Function with using the provided version. Upon success, migrate the supplied alias to it.
   *
   * @param deployMethod supplied by ScalambdaPlugin
   * @param region to deploy lambda function in
   * @param jar to upload as lambda code
   * @param s3Bucket s3 bucket to store source in
   * @param s3KeyPrefix prefix for the s3 bucket
   * @param lambdaName name of lambda (will be combined with lambda handlers)
   * @param handlerName name of lambda handler (will be combined with lambda handlers)
   * @param lambdaHandlers lambda handlers
   * @param versionDescription description for the version, supplied by ScalambdaPlugin
   * @param maybeAlias if supplied, the new version of the function will be pointed to by this alias
   *
   * @return a map of function names to their newly deployed function arns
   */
  def publishLambda(deployMethod: String, region: String, jar: File, s3Bucket: String, s3KeyPrefix: String,
                    lambdaName: Option[String], handlerName: Option[String], lambdaHandlers: Seq[(String, String)],
                    versionDescription: String, maybeAlias: Option[String]): List[QualifiedLambdaArn] = {

    val resolvedRegion = Regions.fromName(region)
    val resolvedLambdaHandlers = resolveLambdaHandlers(lambdaName, handlerName, lambdaHandlers)
    val s3Client = S3.instance(resolvedRegion)
    val lambdaClient = Lambda.instance(resolvedRegion)

    def updateFunctionCode(resolvedLambdaName: LambdaName, updateFunctionCodeRequest: UpdateFunctionCodeRequest): (String, QualifiedLambdaArn) = {
      lambdaClient.updateLambdaWithFunctionCodeRequest(updateFunctionCodeRequest, versionDescription) match {
        case Success(updateResult) =>
          val revisionId = updateResult.getVersion
          lambdaClient.tagLambda(resolvedLambdaName.value, revisionId)
          resolvedLambdaName.value -> QualifiedLambdaArn(resolvedLambdaName.value, revisionId)
        case Failure(exception) =>
          sys.error(s"Error updating lambda: ${formatException(exception)}")
      }
    }

    /**
     * Update Lambda Function(s)
     */

    val updatedLambdas: List[(String, QualifiedLambdaArn)] = deployMethod match {
      case "S3" =>
        val resolvedBucketId = S3BucketId(s3Bucket)
        val resolvedS3KeyPrefix = s3KeyPrefix

        s3Client.pushJarToS3(jar, resolvedBucketId, resolvedS3KeyPrefix) match {
          case Success(s3Key) => (for (resolvedLambdaName <- resolvedLambdaHandlers.keys) yield {
            val updateFunctionCodeRequest = new UpdateFunctionCodeRequest()
              .withFunctionName(resolvedLambdaName.value)
              .withS3Bucket(resolvedBucketId.value)
              .withS3Key(s3Key.value)

            updateFunctionCode(resolvedLambdaName, updateFunctionCodeRequest)
          }).toList
          case Failure(exception) =>
            sys.error(s"Error uploading jar to S3 lambda: ${formatException(exception)}")
        }

      case "DIRECT" =>
        (for (resolvedLambdaName <- resolvedLambdaHandlers.keys) yield {
          val updateFunctionCodeRequest = new UpdateFunctionCodeRequest()
            .withFunctionName(resolvedLambdaName.value)
            .withZipFile(FileOps.fileToBuffer(jar))

          updateFunctionCode(resolvedLambdaName, updateFunctionCodeRequest)
        }).toList

      case unknownDeployMethod: String =>
        sys.error(s"Unsupported deploy method: ${unknownDeployMethod}")
    }

    /**
     * Update Lambda Alias(es)
     */

    val aliasUpdateResult = maybeAlias.map(alias => {
      // create/migrate the supplied Lambda Alias, pointing at each newly deployed version of the lambdas
      updatedLambdas.map({ case (lambdaName, lambdaArn) => lambdaClient.migrateAlias(alias, lambdaArn) }).sequence
    }).getOrElse({
      println("Lambda alias management step was skipped since no `scalambdaAlias` was set.")
      Try(updatedLambdas.map(_._2))
    })

    aliasUpdateResult match {
      case Failure(exception) =>
        sys.error(s"Failed to update Lambda Alias: ${exception}")
      case Success(value) =>
        value
    }

  }

  private def resolveLambdaHandlers(maybeLambdaName: Option[String], maybeHandlerName: Option[String],
                                    lambdaHandlers: Seq[(String, String)]): Map[LambdaName, HandlerName] = {
    val maybeLambda = for {
      lambdaName <- maybeLambdaName
      handlerName <- maybeHandlerName
    } yield {
      Seq(lambdaName -> handlerName)
    }

    val handlers: Seq[(String, String)] = lambdaHandlers

    val allHandlers = maybeLambda.getOrElse(Seq.empty) ++ handlers
    allHandlers.map({ case (l, h) => LambdaName(l) -> HandlerName(h) }).toMap

  }

  private def formatException(t: Throwable): String = {
    val msg = Option(t.getLocalizedMessage).getOrElse(t.toString)
    s"$msg\n${t.getStackTrace.mkString("","\n","\n")}"
  }
}
