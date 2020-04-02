package io.carpe.scalambda.assembly

import io.carpe.scalambda.Log4j2MergeStrategy
import io.carpe.scalambda.ScalambdaPlugin.autoImport.{packageScalambda, packageScalambdaDependencies, packageScalambdaDependenciesMergeStrat, packageScalambdaMergeStrat}
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys.assembledMappings
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.{Assembly, MergeStrategy, PathList}

object AssemblySettings {

  lazy val sourceJarAssemblySettings: Seq[Setting[_]] = Seq(
    // builds the lambda function jar without dependencies (so we can bake them in as a separate lambda layer)
    packageScalambdaMergeStrat := {
      case _ => MergeStrategy.last
    },
    packageScalambda := Assembly.assemblyTask(packageScalambda).value,
    assembledMappings in packageScalambda := Assembly.assembledMappingsTask(packageScalambda).value,
    test in packageScalambda := (test in Test).value,
    assemblyOption in packageScalambda := {
      val ao = (assemblyOption in assembly).value
      ao.copy(includeScala = false, includeDependency = false, mergeStrategy = packageScalambdaMergeStrat.value)
    },
    packageOptions in packageScalambda := (packageOptions in (Compile, packageBin)).value,
    assemblyOutputPath in packageScalambda := {
      (target in assembly).value / (assemblyJarName in packageScalambda).value
    },
    assemblyJarName in packageScalambda := (assemblyJarName in packageScalambda)
      .or(assemblyDefaultJarName in packageScalambda)
      .value,
    assemblyDefaultJarName in packageScalambda := { name.value + "-assembly-" + version.value + ".jar" },
    assemblyOutputPath in packageScalambda := { target.value / "terraform" / "sources.jar" }
  )

  lazy val dependencyAssemblySettings: Seq[Setting[_]] = Seq(
    // builds the dependencies of the lambda version. these will be baked into a lambda layer to improve deployment times
    packageScalambdaDependenciesMergeStrat := {
      case PathList(ps @ _*) if ps.last == "Log4j2Plugins.dat" => Log4j2MergeStrategy.plugincache
      case PathList("META-INF", "MANIFEST.MF")                 => MergeStrategy.discard
      case "log4j2.xml"                                        => MergeStrategy.discard
      case _ =>
        MergeStrategy.last
    },
    packageScalambdaDependencies := Assembly.assemblyTask(packageScalambdaDependencies).value,
    assembledMappings in packageScalambdaDependencies := Assembly
      .assembledMappingsTask(packageScalambdaDependencies)
      .value,
    test in packageScalambdaDependencies := (test in packageScalambda).value,
    assemblyOption in packageScalambdaDependencies := {
      val ao = (assemblyOption in assemblyPackageDependency).value
      ao.copy(
        includeBin = false,
        includeScala = true,
        includeDependency = true,
        appendContentHash = true,
        mergeStrategy = packageScalambdaDependenciesMergeStrat.value
      )
    },
    packageOptions in packageScalambdaDependencies := (packageOptions in (Compile, packageBin)).value,
    assemblyOutputPath in packageScalambdaDependencies := {
      (target in assembly).value / (assemblyJarName in packageScalambdaDependencies).value
    },
    assemblyJarName in packageScalambdaDependencies := (assemblyJarName in packageScalambdaDependencies)
      .or(assemblyDefaultJarName in packageScalambdaDependencies)
      .value,
    assemblyDefaultJarName in packageScalambdaDependencies := {
      name.value + "-assembly-" + version.value + "-deps.jar"
    }
  )
}
