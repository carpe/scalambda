package io.carpe.scalambda.testing

import com.amazonaws.services.lambda.runtime.LambdaLogger

object UnitTestLogger extends LambdaLogger {
  override def log(message: String): Unit      = System.out.println(message)
  override def log(message: Array[Byte]): Unit = System.out.println(new String(message))
}
