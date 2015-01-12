package core

import com.gilt.apidoc.spec.models.Enum

object Util {

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

  def isValidUri(value: String): Boolean = {
    val formatted = value.trim.toLowerCase
    formatted.startsWith("http://") || formatted.startsWith("https://") || formatted.startsWith("file://")
  }

}
