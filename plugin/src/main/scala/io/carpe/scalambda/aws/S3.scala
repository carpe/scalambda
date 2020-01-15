package io.carpe.scalambda.aws

import java.io.File

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.{Region => _, _}

import scala.util.Try
import com.gilt.aws.lambda.{S3BucketId, S3Key}

trait S3 {
  protected def listBuckets(): Try[java.util.List[Bucket]]
  protected def createBucket(bucket: String): Try[Bucket]
  protected def putObject(req: PutObjectRequest): Try[PutObjectResult]

  def pushJarToS3(jar: File, bucketId: S3BucketId, s3KeyPrefix: String): Try[S3Key] = {
    val key = s3KeyPrefix + jar.getName
    val objectRequest = new PutObjectRequest(bucketId.value, key, jar)
      .withCannedAcl(CannedAccessControlList.AuthenticatedRead)

    putObject(objectRequest)
      .map { _ => S3Key(key) }
  }

  def getBucket(bucketId: S3BucketId): Option[Bucket] = {
    import scala.collection.JavaConverters._

    listBuckets()
      .toOption
      .flatMap { _.asScala.find(_.getName == bucketId.value) }
  }

  def createBucket(bucketId: S3BucketId): Try[S3BucketId] = {
    createBucket(bucketId.value)
      .map { _ => bucketId }
  }
}

object S3 {
  def instance(region: Regions): S3 = {
    val auth = new DefaultAWSCredentialsProviderChain()
    val client = AmazonS3ClientBuilder.standard()
      .withCredentials(auth)
      .withRegion(region)
      .build

    new S3 {
      def listBuckets() = Try(client.listBuckets)
      def createBucket(bucket: String) = Try(client.createBucket(bucket))
      def putObject(req: PutObjectRequest) = Try(client.putObject(req))
    }
  }
}
