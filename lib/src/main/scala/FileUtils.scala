package lib

import java.io.File

object FileUtils {

  def readToString(path: String): String = readToString(new File(path))

  def readToString(file: File): String = {
    val source = scala.io.Source.fromFile(file, "UTF-8")
    try {
      source.getLines().mkString("\n").trim
    } finally {
      source.close()
    }
  }

}
