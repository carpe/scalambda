package io.carpe.scalambda.assembly

import io.carpe.scalambda.Log4j2MergeStrategy
import _root_.io.carpe.scalambda.conf.function.{ScalambdaRuntime, _}
import com.typesafe.sbt.packager.graalvmnativeimage.GraalVMNativeImagePlugin.autoImport.GraalVMNativeImage
import io.carpe.scalambda.ScalambdaPlugin.autoImport.{scalambdaDependenciesMergeStrat, scalambdaFunctions, scalambdaPackage, scalambdaPackageDependencies, scalambdaPackageMergeStrat, scalambdaPackageNative, scalambdaTerraformPath}
import sbt.Keys._
import sbt._
import sbt.Def.Initialize
import sbtassembly.AssemblyKeys.assembledMappings
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.{Assembly, MappingSet, MergeStrategy, PathList}

object AssemblySettings {

  lazy val defaultSettings: Seq[sbt.Setting[_]] = nativeImageAssemblySettings ++ functionJarAssemblySettings ++ dependencyAssemblySettings

  // builds the lambda function sources as a native image with graal native
  lazy val nativeImageAssemblySettings: Seq[Setting[_]] = Seq(
    scalambdaPackageNative := {
      // only assembly the native packages if a function with a native runtime exists
      (Def.taskDyn[Option[java.io.File]] {
        val runtimes: Seq[ScalambdaRuntime] = scalambdaFunctions.value.flatMap(_.runtime)

        if (runtimes.contains(ScalambdaRuntime.GraalNative)) {
          Def.task {
            // create the native image
            val nativeImage = ScalambdaAssemblyNative.packageNativeImage("function").value
            Some(nativeImage)
          }
        } else {
          Def.task {
            None
          }
        }
      }).value

    }
  )

  // builds the lambda function jar without dependencies (so we can bake them in as a separate lambda layer)
  lazy val functionJarAssemblySettings: Seq[Setting[_]] = Seq(
    scalambdaPackageMergeStrat := {
      case _ => MergeStrategy.last
    },
    scalambdaPackage := {
      // only assemble the jar if a function with a jvm-based runtime exists
      (Def.taskDyn[Option[java.io.File]] {
        val runtimes: Seq[ScalambdaRuntime] = scalambdaFunctions.value.flatMap(_.runtime)

        if (runtimes.contains(ScalambdaRuntime.Java8) || runtimes.contains(ScalambdaRuntime.Java11)) {
          Def.task {
            val functionJar = scalambdaAssemblyTask(scalambdaPackage).value
            Some(functionJar)
          }
        } else {
          Def.task {
            None
          }
        }
      }).value
    },
    assembledMappings in scalambdaPackage := scalambdaAssembledMappingsTask(scalambdaPackage).value,
    test in scalambdaPackage := (test in Test).value,
    assemblyOption in scalambdaPackage := {
      val ao = (assemblyOption in assembly).value
      ao.copy(includeScala = false, includeDependency = false, mergeStrategy = scalambdaPackageMergeStrat.value)
    },
    packageOptions in scalambdaPackage := (packageOptions in(Compile, packageBin)).value,
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
      case PathList(ps@_*) if ps.last == "Log4j2Plugins.dat" => Log4j2MergeStrategy.plugincache
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case "log4j2.xml" => MergeStrategy.discard
      case "reference.conf" => MergeStrategy.concat
      case _ =>
        MergeStrategy.last
    },
    scalambdaPackageDependencies := {
      // only assemble the jar if a function with a jvm-based runtime exists
      (Def.taskDyn[Option[java.io.File]] {
        val runtimes: Seq[ScalambdaRuntime] = scalambdaFunctions.value.flatMap(_.runtime)

        if (runtimes.contains(ScalambdaRuntime.Java8) || runtimes.contains(ScalambdaRuntime.Java11)) {
          Def.task {
            val dependenciesJar = ScalambdaAssemblyJVM.assembleLambdaLayerTask.value
            Some(dependenciesJar)
          }
        } else {
          Def.task {
            None
          }
        }
      }).value
    },
    assembledMappings in scalambdaPackageDependencies := scalambdaAssembledMappingsTask(scalambdaPackageDependencies)
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
    packageOptions in scalambdaPackageDependencies := (packageOptions in(Compile, packageBin)).value,
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

  def scalambdaAssemblyTask(key: TaskKey[_]): Initialize[Task[java.io.File]] = Def.task {
    val t = (test in key).value
    val s = (streams in key).value
    Assembly(
      (assemblyOutputPath in key).value, (assemblyOption in key).value,
      (packageOptions in key).value, (assembledMappings in key).value,
      s.cacheDirectory, s.log)
  }
  def scalambdaAssembledMappingsTask(key: TaskKey[_]): Initialize[Task[Seq[MappingSet]]] = Def.task {
    val s = (streams in key).value
    Assembly.assembleMappings(
      (fullClasspath in assembly).value, (externalDependencyClasspath in assembly).value,
      (assemblyOption in key).value, s.log)
  }
}
