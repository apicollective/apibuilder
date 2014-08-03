package core

import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets

object TestHelper {

  def writeToFile(path: String, contents: String) {
    val outputPath = Paths.get(path)
    val bytes = contents.getBytes(StandardCharsets.UTF_8)
    Files.write(outputPath, bytes)
  }

  def  readFile(path: String): String = {
    scala.io.Source.fromFile(path).getLines.mkString("\n")
  }

  def parseFile(filename: String): ServiceDescriptionValidator = {
    val contents = readFile(filename)
    val validator = ServiceDescriptionValidator(contents)
    if (!validator.isValid) {
      sys.error(s"Invalid api.json file[${filename}]: " + validator.errors.mkString("\n"))
    }
    validator
  }

  def assertEqualsFile(filename: String, contents: String) {
    if (contents.trim != readFile(filename).trim) {
      val tmpPath = "/tmp/apidoc.tmp"
      TestHelper.writeToFile(tmpPath, contents.trim)
      sys.error(s"Test output did not match. diff $filename $tmpPath")
    }
  }

}
