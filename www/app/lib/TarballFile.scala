package lib

import java.io.{BufferedOutputStream, FileOutputStream}

import com.bryzek.apidoc.generator.v0.models.File
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream

object TarballFile {

  def create(dirName: String, files: Seq[File]): java.io.File = {
    val path = java.io.File.createTempFile(dirName, ".tar.gz")
    val tar: TarArchiveOutputStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(path))))

    files.foreach { file =>
      val entry = new TarArchiveEntry(new java.io.File(file.name), s"$dirName/${file.name}")
      entry.setSize(file.contents.getBytes.length)
      tar.putArchiveEntry(entry)
      tar.write(file.contents.getBytes("UTF-8"))
      tar.closeArchiveEntry()
    }
    tar.close()

    path
  }

  def splitFileByExtension(file: File): (String, String) = {
    file.name.splitAt(file.name.lastIndexOf('.'))
  }
}
