package models

import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets

import lib.Text
import core.ServiceValidator

object TestHelper {

  def writeToFile(path: String, contents: String) {
    val outputPath = Paths.get(path)
    val bytes = contents.getBytes(StandardCharsets.UTF_8)
    Files.write(outputPath, bytes)
  }

  def  readFile(path: String): String = {
    scala.io.Source.fromFile(path).getLines.mkString("\n")
  }

  def parseFile(filename: String): ServiceValidator = {
    val contents = readFile(filename)
    val validator = ServiceValidator(contents)
    if (!validator.isValid) {
      sys.error(s"Invalid api.json file[${filename}]: " + validator.errors.mkString("\n"))
    }
    validator
  }

  def assertEqualsFile(filename: String, contents: String) {
    if (contents.trim != readFile(filename).trim) {
      val tmpPath = "/tmp/apidoc.tmp." + Text.safeName(filename)
      TestHelper.writeToFile(tmpPath, contents.trim)
      sys.error(s"Test output did not match. diff $tmpPath $filename")
    }
  }

}
