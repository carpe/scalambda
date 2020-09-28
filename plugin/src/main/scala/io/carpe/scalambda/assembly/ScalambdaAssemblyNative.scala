package io.carpe.scalambda.assembly

import java.nio.file.Files
import java.nio.file.attribute.{PosixFileAttributes, PosixFilePermission}

import com.typesafe.sbt.packager.graalvmnativeimage.GraalVMNativeImagePlugin.autoImport.GraalVMNativeImage
import io.carpe.scalambda.ScalambdaPlugin.autoImport.{scalambdaPackageDependencies, scalambdaTerraformPath}
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.utils.IOUtils
import sbt.{Def, File, IO, Task}
import sbt.Keys.packageBin


object ScalambdaAssemblyNative {

  protected[scalambda] val assembleDepsKey = scalambdaPackageDependencies

  /**
   * This function puts packages the native image into a zip so that the lambda can use it.
   *
   * @return
   */
  def packageNativeImage(functionName: String): Def.Initialize[Task[java.io.File]] = Def.task {
    import sbt._

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
    val archiveStream = new FileOutputStream(zipOutput.getAbsolutePath)
    val archive = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, archiveStream)

    // add executable as bootstrap executable
    val bootstrap = new ZipArchiveEntry("bootstrap")
    bootstrap.setUnixMode(755)
    archive.putArchiveEntry(bootstrap)

    val input = new BufferedInputStream(new FileInputStream(nativeImage))

    IOUtils.copy(input, archive)
    input.close()
    archive.closeArchiveEntry()

    archive.finish()
    archiveStream.close()
  }
}
