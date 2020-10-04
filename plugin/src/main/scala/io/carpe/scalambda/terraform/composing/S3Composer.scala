package io.carpe.scalambda.terraform.composing


import io.carpe.scalambda.conf.function.FunctionSources
import io.carpe.scalambda.terraform.ast.Definition.Variable
import io.carpe.scalambda.terraform.ast.props.TValue.{TLiteral, TObject}
import io.carpe.scalambda.terraform.ast.providers.aws.BillingTag
import io.carpe.scalambda.terraform.ast.providers.aws.s3.{S3Bucket, S3BucketItem}

object S3Composer {

  case class S3Composable(functionSources: FunctionSources, s3BucketName: String, billingTags: Seq[BillingTag]) {
    lazy val containsJvmRuntime: Boolean = functionSources.functionJar.isDefined
    lazy val containsNativeRuntime: Boolean = functionSources.nativeImage.isDefined
  }

  case class S3Resources(bucket: S3Bucket, sourcesJarBucketItem: Option[S3BucketItem], dependencyBucketItem: Option[S3BucketItem], nativeImageBucketItem: Option[S3BucketItem], s3BillingTagsVariable: Variable[TObject]) {
    lazy val bucketItems: Seq[S3BucketItem] = Seq(sourcesJarBucketItem, dependencyBucketItem, nativeImageBucketItem).flatten
  }

  def defineS3Resources(s3Composable: S3Composable): S3Resources = {
    import s3Composable._

    val baseResources: S3Resources = {
      // get the user's configured bucket name
      val bucketName: String = s3BucketName

      val additionalBillingTagsVariable: Variable[TObject] = Variable[TObject](
        name = "s3_billing_tags",
        description = Some("AWS Billing tags to add to the S3 bucket that contains the compiled sources for your functions"),
        // set default to empty object
        defaultValue = Some(TObject())
      )

      // create a new bucket
      val newBucket = S3Bucket(bucketName, billingTags, additionalBillingTagsVariable.ref)

      // use bucket to create base set of resources
      S3Resources(newBucket, None, None, None, additionalBillingTagsVariable)
    }


    // add jvm resources if there are any lambda functions with a jvm-based runtime
    val createResourcesWithJvm: S3Resources = {
      if (containsJvmRuntime) {
        defineS3ResourcesForJVMRuntimes(baseResources, billingTags)
      } else {
        baseResources
      }
    }

    // add native resources if there are any lambda functions with a native runtime
    if (containsNativeRuntime) {
      defineS3ResourcesForGraalNativeRuntimes(createResourcesWithJvm, s3Composable)
    } else {
      createResourcesWithJvm
    }
  }

  private def defineS3ResourcesForGraalNativeRuntimes(base: S3Resources, s3Composable: S3Composable): S3Resources = {
    val nativeImageBucketItem = S3BucketItem(
      base.bucket,
      name = "sources",
      key = "function.zip",
      source = """function.zip""",
      etag = TLiteral("""filemd5("${path.module}/function.zip")"""),
      billingTags = s3Composable.billingTags
    )

    base.copy(nativeImageBucketItem = Some(nativeImageBucketItem))
  }

  private def defineS3ResourcesForJVMRuntimes(base: S3Resources, billingTags: Seq[BillingTag]): S3Resources = {

    // create bucket items (to be placed into that bucket) that are pointed to the sources of each lambda function
    val sourceBucketItem =
      S3BucketItem(
        base.bucket,
        name = "sources",
        key = "sources.jar",
        source = "sources.jar",
        etag = TLiteral("""filemd5("${path.module}/sources.jar")"""),
        billingTags = billingTags
      )

    val depsBucketItem = S3BucketItem(
      base.bucket,
      name = "dependencies",
      key = "dependencies.zip",
      source = "dependencies.zip",
      etag = TLiteral("""filemd5("${path.module}/dependencies.zip")"""),
      billingTags = billingTags
    )

    // copy the resources into the base
    base.copy(sourcesJarBucketItem = Some(sourceBucketItem), dependencyBucketItem = Some(depsBucketItem))
  }
}