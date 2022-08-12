package io.carpe.scalambda.assemble

import _root_.io.carpe.scalambda.conf.function.ScalambdaRuntime
import io.carpe.scalambda.Log4j2MergeStrategy
import io.carpe.scalambda.ScalambdaPlugin.autoImport.*
import sbt.Def.Initialize
import sbt.Keys.*
import sbt.*
import sbtassembly.AssemblyKeys.assembledMappings
import sbtassembly.AssemblyPlugin.autoImport.*
import sbtassembly.{Assembly, AssemblyOption, MappingSet, MergeStrategy, PathList}

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
    scalambdaPackageMergeStrat := sbtassembly.MergeStrategy.defaultMergeStrategy,
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
    scalambdaPackage / assembledMappings := scalambdaAssembledMappingsTask(scalambdaPackage).value,
    scalambdaPackage / test := (Test / test).value,
    scalambdaPackage / assemblyOption :=
      (assembly / assemblyOption).value
        .withIncludeScala(false)
        .withIncludeDependency(false)
        .withMergeStrategy(scalambdaPackageMergeStrat.value),
    scalambdaPackage / packageOptions := (Compile / packageBin / packageOptions).value,
    scalambdaPackage / assemblyOutputPath := {
      (assembly / target).value / (scalambdaPackage / assemblyJarName).value
    },
    scalambdaPackage / assemblyJarName := (scalambdaPackage / assemblyJarName)
      .or(scalambdaPackage / assemblyDefaultJarName)
      .value,
    scalambdaPackage / assemblyDefaultJarName := { name.value + "-assembly-" + version.value + ".jar" },
    scalambdaPackage / assemblyOutputPath := { scalambdaTerraformPath.value / "sources.jar" }
  )


  // builds the dependencies of the lambda version. these will be baked into a lambda layer to improve deployment times
  lazy val dependencyAssemblySettings: Seq[Setting[_]] = Seq(
    scalambdaDependenciesMergeStrat := sbtassembly.MergeStrategy.defaultMergeStrategy,
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
    scalambdaPackageDependencies / assembledMappings := scalambdaAssembledMappingsTask(scalambdaPackageDependencies)
      .value,
    scalambdaPackageDependencies / test := (scalambdaPackage / test).value,
    scalambdaPackageDependencies / assemblyOption :=
      (assemblyPackageDependency / assemblyOption).value
        .withIncludeBin(false)
        .withIncludeScala(true)
        .withIncludeDependency(true)
        .withAppendContentHash(true)
        .withMergeStrategy(scalambdaDependenciesMergeStrat.value),
    scalambdaPackageDependencies / packageOptions := (Compile / packageBin / packageOptions).value,
    scalambdaPackageDependencies / assemblyOutputPath := {
      (assembly / target).value / (scalambdaPackageDependencies / assemblyJarName).value
    },
    scalambdaPackageDependencies / assemblyJarName := (scalambdaPackageDependencies / assemblyJarName)
      .or(scalambdaPackageDependencies / assemblyDefaultJarName)
      .value,
    scalambdaPackageDependencies / assemblyDefaultJarName := {
      name.value + "-assembly-" + version.value + "-deps.jar"
    }
  )

  def scalambdaAssemblyTask(key: TaskKey[_]): Initialize[Task[java.io.File]] = Def.task {
    (key / test).value
    val s = (key / streams).value
    Assembly(
      (key / assemblyOutputPath).value, (key / assemblyOption).value,
      (key / packageOptions).value, (key / assembledMappings).value,
      s.cacheDirectory, s.log)
  }
  def scalambdaAssembledMappingsTask(key: TaskKey[_]): Initialize[Task[Seq[MappingSet]]] = Def.task {
    val s = (key / streams).value
    Assembly.assembleMappings(
      (assembly / fullClasspath).value, (assembly / externalDependencyClasspath).value,
      (key / assemblyOption).value, s.log)
  }
}
