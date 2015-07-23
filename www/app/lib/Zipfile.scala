package lib

import java.io.FileOutputStream
import java.util.zip.{ZipEntry, ZipOutputStream}
import com.bryzek.apidoc.generator.v0.models.File

object Zipfile {

  private[this] val UTF8 = "UTF-8"

  def create(dirName: String, files: Seq[File]): java.io.File = {
    val path = java.io.File.createTempFile(dirName, ".zip")
    createForFile(path, files, prefix = dirName + "/")
    path
  }

  private def createForFile(
    zip: java.io.File,
    files: Seq[File],
    prefix: String = ""
  ) {
    val zipOutputStream = new ZipOutputStream(new FileOutputStream(zip))
    files.foreach { f =>
      zipOutputStream.putNextEntry(new ZipEntry(s"$prefix${f.name}"))
      zipOutputStream.write(f.contents.getBytes(UTF8))
      zipOutputStream.closeEntry
    }
    zipOutputStream.close()
  }


}
