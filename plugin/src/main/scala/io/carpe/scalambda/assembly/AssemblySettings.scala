package io.carpe.scalambda.assembly

import io.carpe.scalambda.Log4j2MergeStrategy
import io.carpe.scalambda.ScalambdaPlugin.autoImport.{scalambdaTerraformPath, scalambdaPackage, scalambdaPackageDependencies, scalambdaDependenciesMergeStrat, scalambdaPackageMergeStrat}
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys.assembledMappings
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.{Assembly, MergeStrategy, PathList}

object AssemblySettings {

  // builds the lambda function jar without dependencies (so we can bake them in as a separate lambda layer)
  lazy val sourceJarAssemblySettings: Seq[Setting[_]] = Seq(
    scalambdaPackageMergeStrat := {
      case _ => MergeStrategy.last
    },
    scalambdaPackage := Assembly.assemblyTask(scalambdaPackage).value,
    assembledMappings in scalambdaPackage := Assembly.assembledMappingsTask(scalambdaPackage).value,
    test in scalambdaPackage := (test in Test).value,
    assemblyOption in scalambdaPackage := {
      val ao = (assemblyOption in assembly).value
      ao.copy(includeScala = false, includeDependency = false, mergeStrategy = scalambdaPackageMergeStrat.value)
    },
    packageOptions in scalambdaPackage := (packageOptions in (Compile, packageBin)).value,
    assemblyOutputPath in scalambdaPackage := {
      (target in assembly).value / (assemblyJarName in scalambdaPackage).value
    },
    assemblyJarName in scalambdaPackage := (assemblyJarName in scalambdaPackage)
      .or(assemblyDefaultJarName in scalambdaPackage)
      .value,
    assemblyDefaultJarName in scalambdaPackage := { name.value + "-assembly-" + version.value + ".jar" },
    assemblyOutputPath in scalambdaPackage := { scalambdaTerraformPath.value / "sources.jar" }
  )


  // builds the dependencies of the lambda version. these will be baked into a lambda layer to improve deployment times
  lazy val dependencyAssemblySettings: Seq[Setting[_]] = Seq(
    scalambdaDependenciesMergeStrat := {
      case PathList(ps @ _*) if ps.last == "Log4j2Plugins.dat" => Log4j2MergeStrategy.plugincache
      case PathList("META-INF", "MANIFEST.MF")                 => MergeStrategy.discard
      case "log4j2.xml"                                        => MergeStrategy.discard
      case "reference.conf"                                    => MergeStrategy.concat
      case _ =>
        MergeStrategy.last
    },
    scalambdaPackageDependencies := ScalambdaAssembly.assembleLambdaLayerTask.value,
    assembledMappings in scalambdaPackageDependencies := Assembly
      .assembledMappingsTask(scalambdaPackageDependencies)
      .value,
    test in scalambdaPackageDependencies := (test in scalambdaPackage).value,
    assemblyOption in scalambdaPackageDependencies := {
      val ao = (assemblyOption in assemblyPackageDependency).value
      ao.copy(
        includeBin = false,
        includeScala = true,
        includeDependency = true,
        appendContentHash = true,
        mergeStrategy = scalambdaDependenciesMergeStrat.value
      )
    },
    packageOptions in scalambdaPackageDependencies := (packageOptions in (Compile, packageBin)).value,
    assemblyOutputPath in scalambdaPackageDependencies := {
      (target in assembly).value / (assemblyJarName in scalambdaPackageDependencies).value
    },
    assemblyJarName in scalambdaPackageDependencies := (assemblyJarName in scalambdaPackageDependencies)
      .or(assemblyDefaultJarName in scalambdaPackageDependencies)
      .value,
    assemblyDefaultJarName in scalambdaPackageDependencies := {
      name.value + "-assembly-" + version.value + "-deps.jar"
    }
  )
}
