package io.carpe.scalambda.terraform.ast.resources

import io.carpe.scalambda.fixtures.TerraformBehaviors
import io.carpe.scalambda.terraform.ast.providers.aws.iam.Role
import org.scalatest.flatspec.AnyFlatSpec

class IamRoleSpec extends AnyFlatSpec with TerraformBehaviors {

  ("IAM Role" should behave).like(
    printableTerraform(
      Role(
        "MyCloudWatchRule",
        """{
          |  "Version": "2012-10-17",
          |  "Statement": [
          |    {
          |      "Action": "sts:AssumeRole",
          |      "Principal": {
          |        "Service": "ec2.amazonaws.com"
          |      },
          |      "Effect": "Allow",
          |      "Sid": ""
          |    }
          |  ]
          |}""".stripMargin,
        "Role for testing"
      ), {
        """resource "aws_iam_role" "my_cloud_watch_rule" {
          |  name_prefix = "MyCloudWatchRule"
          |  assume_role_policy = <<EOF
          |{
          |  "Version": "2012-10-17",
          |  "Statement": [
          |    {
          |      "Action": "sts:AssumeRole",
          |      "Principal": {
          |        "Service": "ec2.amazonaws.com"
          |      },
          |      "Effect": "Allow",
          |      "Sid": ""
          |    }
          |  ]
          |}
          |EOF
          |}""".stripMargin
      }
    )
  )
}
