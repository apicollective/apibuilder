package core

import codegenerator.models.Enum

object Util {

  val MethodsNotAcceptingBodies = Seq("GET", "DELETE")

  def isJsonDocumentMethod(verb: String): Boolean = {
    !MethodsNotAcceptingBodies.contains(verb.toUpperCase)
  }

  // Select out named parameters in the path. E.g. /:org/:service/foo would return [org, service]
  def namedParametersInPath(path: String): Seq[String] = {
    path.split("/").flatMap { name =>
      if (name.startsWith(":")) {
        Some(name.slice(1, name.length))
      } else {
        None
      }
    }
  }

  def isValidEnumValue(enum: Enum, value: String): Boolean = {
    enum.values.map(_.name).contains(value)
  }

  def assertValidEnumValue(enum: Enum, value: String) {
    require(isValidEnumValue(enum, value), s"Enum[${enum.name}] does not have a value[${value}]. Valid values are: " + enum.values.map(_.name).mkString(", "))
  }

}
