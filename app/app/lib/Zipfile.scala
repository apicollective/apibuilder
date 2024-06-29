package lib

import java.io.FileOutputStream
import java.util.zip.{ZipEntry, ZipOutputStream}
import io.apibuilder.generator.v0.models.File

object Zipfile {

  private val UTF8 = "UTF-8"

  def create(dirName: String, files: Seq[File]): java.io.File = {
    val path = java.io.File.createTempFile(dirName, ".zip")
    createForFile(path, files, prefix = dirName + "/")
    path
  }

  private def createForFile(
    zip: java.io.File,
    files: Seq[File],
    prefix: String
  ): Unit = {
    val zipOutputStream = new ZipOutputStream(new FileOutputStream(zip))
    files.foreach { f =>
      val path = prefix + f.dir.fold("")(_ + "/")
      zipOutputStream.putNextEntry(new ZipEntry(s"$path${f.name}"))
      zipOutputStream.write(f.contents.getBytes(UTF8))
      zipOutputStream.closeEntry
    }
    zipOutputStream.close()
  }


}
