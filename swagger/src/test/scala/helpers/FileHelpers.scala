package io.apibuilder.swagger.helpers

trait FileHelpers {
  private val resourcesDir = "swagger/src/test/resources/"

  def readResource(name: String): String = {
    readFile(resourcesDir + name)
  }

  def readFile(path: String): String = {
    val source = scala.io.Source.fromFile(path)
    try {
      source.getLines().mkString("\n")
    } finally {
      source.close()
    }
  }
}