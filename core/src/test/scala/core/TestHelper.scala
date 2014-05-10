package core

object TestHelper {

  def readFile(path: String): String = {
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

}
