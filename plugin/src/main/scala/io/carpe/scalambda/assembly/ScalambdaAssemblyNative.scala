package io.carpe.scalambda.assembly

import com.typesafe.sbt.packager.graalvmnativeimage.GraalVMNativeImagePlugin.autoImport.GraalVMNativeImage
import io.carpe.scalambda.ScalambdaPlugin.autoImport.{scalambdaPackageDependencies, scalambdaTerraformPath}
import sbt.Keys.{packageBin, packageOptions, streams}
import sbt.Tracked
import sbt.util.FileInfo.exists
import sbtassembly.AssemblyPlugin.autoImport.{assembledMappings, assemblyOption, assemblyOutputPath}
import sbtassembly.{Assembly, AssemblyOption, MappingSet}


object ScalambdaAssemblyNative {

  import Tracked.{inputChanged, outputChanged}
  import sbt._

  protected[scalambda] val assembleDepsKey = scalambdaPackageDependencies

  /**
   * This function puts packages the native image into a zip so that the lambda can use it.
   *
   * @return
   */
  def packageNativeImage(functionName: String): Def.Initialize[Task[java.io.File]] = Def.task {
    val outputPath = scalambdaTerraformPath.value / s"${functionName}.zip"

    // assembly the native image
    val nativeImage = (GraalVMNativeImage / packageBin).value

    // delegate jar assembly to sbt-assembly
    createArchive(outputPath, nativeImage)

    // return the output path for the zip file
    outputPath
  }

  private def createArchive( zipOutput: File,
                             nativeImage: File
                           ): Unit = {
    import java.io.{BufferedInputStream, FileInputStream, FileOutputStream}
    import java.util.zip.{ZipEntry, ZipOutputStream}

    // create parent directory if it does not already exist
    IO.createDirectory(zipOutput.getParentFile)

    // write zip
    val zip = new ZipOutputStream(new FileOutputStream(zipOutput.getAbsolutePath))
    zip.putNextEntry(new ZipEntry("bootstrap"))
    val in = new BufferedInputStream(new FileInputStream(nativeImage))
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
