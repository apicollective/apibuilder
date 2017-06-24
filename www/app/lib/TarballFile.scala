package lib

import java.io.{BufferedOutputStream, FileOutputStream}

import io.apibuilder.generator.v0.models.File
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream

case object TarballFile {

  def create(prefix: String, files: Seq[File]): java.io.File = {
    val tmpFilePath = java.io.File.createTempFile(prefix, ".tar.gz")
    val tar: TarArchiveOutputStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFilePath))))

    files.foreach { file =>
      val path = prefix + "/" + file.dir.fold("")(_ + "/")

      val entry = new TarArchiveEntry(new java.io.File(file.name), s"$path${file.name}")
      entry.setSize(file.contents.getBytes.length)
      tar.putArchiveEntry(entry)
      tar.write(file.contents.getBytes("UTF-8"))
      tar.closeArchiveEntry()
    }
    tar.close()

    tmpFilePath
  }

}
