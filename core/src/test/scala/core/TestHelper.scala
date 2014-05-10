package core

object TestHelper {

  def readFile(path: String): String = {
    scala.io.Source.fromFile(path).getLines.mkString("\n")
  }

  def parseFile(filename: String): ServiceDescriptionValidator = {
    val contents = readFile(filename)
    ServiceDescriptionValidator(contents)
  }

}
