package core

import com.gilt.apidoc.spec.v0.models.Enum

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

  def validateUri(value: String): Seq[String] = {
    val formatted = value.trim.toLowerCase
    if (!formatted.startsWith("http://") && !formatted.startsWith("https://") && !formatted.startsWith("file://")) {
      Seq(s"URI[$value] must start with http://, https://, or file://")
    } else if (formatted.endsWith("/")) {
      Seq(s"URI[$value] cannot end with a '/'")
    } else {
      Seq.empty
    }
  }

}
