package core

import io.apibuilder.spec.v0.models.Enum
import java.net.{MalformedURLException, URL}
import scala.util.{Failure, Success, Try}

object Util {

  def joinPaths(resourcePath: String, operationPath: Option[String]): String = {
    operationPath match {
      case None => resourcePath
      case Some(op) => {
        if (resourcePath.endsWith("/")) {
          resourcePath + op
        } else {
          resourcePath + "/" + op
        }
      }
    }
  }

  // Select out named parameters in the path. E.g. /:org/:service/foo would return [org, service]
  def namedParametersInPath(path: String): Seq[String] = {
    path.split("/").toSeq.flatMap { name =>
      if (name.startsWith(":")) {
        val idx = if (name.indexOf(".") >= 0) {
          name.indexOf(".")
        } else {
          name.length
        }
        Some(name.slice(1, idx))
      } else {
        None
      }
    }
  }

  def isValidEnumValue(`enum`: Enum, value: String): Boolean = {
    `enum`.values.map(_.name).contains(value)
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
      Try(new URL(value)) match {
        case Success(_) => Nil
        case Failure(e) => e match {
          case e: MalformedURLException => Seq(s"URL is not valid: ${e.getMessage}")
        }
      }
    }
  }

}
