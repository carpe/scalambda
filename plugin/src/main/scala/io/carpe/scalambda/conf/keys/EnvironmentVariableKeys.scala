package io.carpe.scalambda.conf.keys

import io.carpe.scalambda.conf.function.EnvironmentVariable

trait EnvironmentVariableKeys {
  /**
   * Injects an Environment Variable into the Lambda Function's runtime environment, so that you can access it during
   * execution.
   *
   * @param key for the env variable
   * @param value of the env variable
   * @return
   */
  def StaticVariable(key: String, value: String): EnvironmentVariable.StaticVariable = EnvironmentVariable.StaticVariable(key, value)


  def VariableFromTF(key: String, terraformVariableName: String): EnvironmentVariable.VariableFromTF = EnvironmentVariable.VariableFromTF(key, terraformVariableName)
}
