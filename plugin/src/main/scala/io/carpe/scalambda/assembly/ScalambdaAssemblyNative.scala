package io.carpe.scalambda.assembly

import java.nio.file.Files
import java.nio.file.attribute.{PosixFileAttributes, PosixFilePermission}

import com.typesafe.sbt.packager.graalvmnativeimage.GraalVMNativeImagePlugin.autoImport.GraalVMNativeImage
import io.carpe.scalambda.ScalambdaPlugin.autoImport.{scalambdaPackageDependencies, scalambdaTerraformPath}
import sbt.{Def, Task, File, IO}
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

    // set the native image to be readable and executable. this allows for execution
    val imagePath = nativeImage.toPath
    val perms = Files.readAttributes(imagePath, classOf[PosixFileAttributes]).permissions()
    perms.add(PosixFilePermission.OWNER_WRITE)
    perms.add(PosixFilePermission.OWNER_READ)
    perms.add(PosixFilePermission.OWNER_EXECUTE)
    perms.add(PosixFilePermission.GROUP_WRITE)
    perms.add(PosixFilePermission.GROUP_READ)
    perms.add(PosixFilePermission.GROUP_EXECUTE)
    perms.add(PosixFilePermission.OTHERS_WRITE)
    perms.add(PosixFilePermission.OTHERS_READ)
    perms.add(PosixFilePermission.OTHERS_EXECUTE)
    Files.setPosixFilePermissions(imagePath, perms)

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
