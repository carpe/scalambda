package io.carpe.scalambda.assemble

import java.io.File
import java.security.MessageDigest

import io.carpe.scalambda.ScalambdaPlugin.autoImport.{scalambdaPackageDependencies, scalambdaTerraformPath}
import sbt.Keys.{packageOptions, streams}
import sbt.io.Hash
import sbt.util.FileInfo.exists
import sbt.{Def, Logger, PlainFileInfo, Tracked, _}
import sbtassembly.Assembly.applyStrategies
import sbtassembly.AssemblyPlugin.autoImport.{assembledMappings, assemblyOption, assemblyOutputPath}
import sbtassembly.{Assembly, AssemblyOption, MappingSet, MergeStrategy}

object ScalambdaAssemblyJVM {

  import Tracked.{inputChanged, outputChanged}

  protected[scalambda] val assembleDepsKey = scalambdaPackageDependencies

  /**
   * This function exists purely to provide a caching mechanism to the costly operation that is assembling a lambda
   * layer that contains the project's dependencies.
   * @return
   */
  lazy val assembleLambdaLayerTask: Def.Initialize[Task[File]] = Def.task {
    val s = (streams in assembleDepsKey).value
    val cacheDir = s.cacheDirectory
    val log = s.log
    val outputPath = scalambdaTerraformPath.value / "dependencies.zip"

    // delegate jar assembly to sbt-assembly
    createArchiveIfNonExists(
      outputPath,
      (assemblyOutputPath in assembleDepsKey).value,
      (assemblyOption in assembleDepsKey).value,
      (packageOptions in assembleDepsKey).value,
      (assembledMappings in assembleDepsKey).value,
      cacheDir,
      log
    )

    // return the output path for the zip file
    outputPath
  }

  private def createArchiveIfNonExists(
    zipOutputPath: File,
    jarOutputPath: File,
    ao: AssemblyOption,
    po: Seq[PackageOption],
    mappings: Seq[MappingSet],
    cacheDir: File,
    log: Logger
  ): Unit = {

    def sha1 = MessageDigest.getInstance("SHA-1")
    def bytesToString(bytes: Seq[Byte]): String = bytes.map({ "%02x".format(_) }).mkString

    lazy val (ms: Vector[(File, String)], stratMapping: List[(String, MergeStrategy)]) = {
      log.debug("Merging files...")
      applyStrategies(mappings, ao.mergeStrategy, ao.assemblyDirectory, log)
    }

    lazy val inputs = {
      log.debug("Checking every *.class/*.jar file's SHA-1.")
      val rawHashBytes =
        mappings.toVector.par.flatMap { m =>
          m.sourcePackage match {
            case Some(x) => Hash(x)
            case _ =>
              m.mappings.flatMap { x =>
                Hash(x._1)
              }
          }
        }
      val pathStratBytes =
        stratMapping.par.flatMap {
          case (path, strat) =>
            (path + strat.name).getBytes("UTF-8")
        }
      sha1.digest((rawHashBytes.seq ++ pathStratBytes.seq).toArray)
    }

    import CacheImplicits._
    val cachedMakeLayer = inputChanged(cacheDir / "scalambda-dep-assembly-inputs") { (inChanged, inputs: Seq[Byte]) =>
      outputChanged(cacheDir / "scalambda-dep-assembly-outputs") { (outChanged, jar: PlainFileInfo) =>
        if (inChanged) {
          log.debug("SHA-1: " + bytesToString(inputs))
        }
        if (inChanged || outChanged) {
          createArchive(zipOutputPath, jarOutputPath, ao, po, mappings, cacheDir, log)
        } else log.info("Dependency Lambda Layer up to date: " + jar.file)
      }
    }
    cachedMakeLayer(inputs)(() => exists(jarOutputPath))
  }

  private def createArchive(
    zipOutput: File,
    jarOut: File,
    ao: AssemblyOption,
    po: Seq[PackageOption],
    mappings: Seq[MappingSet],
    cacheDir: File,
    log: Logger
  ): Unit = {
    // delegate dependency jar assembly to sbt-assembly
    val dependencyJar = Assembly(jarOut, ao, po, mappings, cacheDir, log)

    // we need to store the dependencies in a specific folder structure in order to have them loaded into the lambda layer
    import java.io.{BufferedInputStream, FileInputStream, FileOutputStream}
    import java.util.zip.{ZipEntry, ZipOutputStream}

    // create zip file of dependencies jar because terraform's archive_file is acting strange in recent versions
    // TODO: review this and see if we can switch back to using a terraform archive

    // create parent directory if it does not already exist
    IO.createDirectory(zipOutput.getParentFile)

    // write zip
    val zip = new ZipOutputStream(new FileOutputStream(zipOutput.getAbsolutePath))
    zip.putNextEntry(new ZipEntry("java/lib/dependencies.jar"))
    val in = new BufferedInputStream(new FileInputStream(dependencyJar))
    var b = in.read()
    while (b > -1) {
      zip.write(b)
      b = in.read()
    }
    in.close()
    zip.closeEntry()
    zip.close()
  }

}
