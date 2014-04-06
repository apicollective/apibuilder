package lib

import core.{ Field, Operation, Resource, ServiceDescription }
import io.Source
import scala.collection.immutable.StringOps

object Util {

  def scalaDataType(param: Field): String = {
    val scalaType = param.dataType match {
      case "string" => "String"
      case "long" => "Long"
      case "integer" => "Int"
      case "boolean" => "Boolean"
      case _ => {
        sys.error(s"Cannot map data type[${param.dataType}] to scala type")
      }
    }

    if (param.required) {
      scalaType
    } else {
      s"Option[$scalaType]"
    }
  }

}

/**
 * Generates a Play routes file based on the service description
 * from api.json
 */
case class RouteGenerator(json: String) {

  private val GlobalPad = 5

  private lazy val service = ServiceDescription(json)

  def generate(): String = {
    val all = service.resources.flatMap { resource =>
      resource.operations.map { op =>
        Route(resource, op)
      }
    }
    val maxVerbLength = all.map(_.verb.length).sorted.last
    val maxUrlLength = all.map(_.url.length).sorted.last

    all.map { r =>
      Seq(
        r.verb,
        " " * (maxVerbLength - r.verb.length + GlobalPad),
        r.url,
        " " * (maxUrlLength - r.url.length + GlobalPad),
        r.method,
        "(",
        r.parameters.mkString(", "),
        ")"
      ).mkString("")
    }.mkString("\n")
  }

  private[this] case class Route(resource: Resource, op: Operation) {

    lazy val verb = op.method
    lazy val url = if (service.basePath.isEmpty && resource.path.isEmpty && op.path.isEmpty) {
      "/"
    } else {
      service.basePath.getOrElse("") + resource.path + op.path.getOrElse("")
    }
    lazy val method = {
      s"$controllerName.$methodName"
    }
    lazy val parameters = {
      op.parameters.map { param =>
        Seq(
          Some(s"${param.name}: ${Util.scalaDataType(param)}"),
          param.default.map( d => s"?= ${d}" )
        ).flatten.mkString(" ")
      }
    }

    private lazy val methodName = {
      op.path match {
        case None => {
          op.method.toLowerCase
        }

        case Some(path: String) => {
          op.method.toLowerCase + initCap(path.split("/"))
        }
      }
    }

    private lazy val controllerName: String = {
      s"controllers.${initCap(resource.name)}"
    }

    private val Regexp = """([^0-9a-z])""".r

    private def initCap(word: String): String = {
      val safeWord = Regexp.replaceAllIn(word.toLowerCase.trim, m => "").trim.toLowerCase
      new StringOps(safeWord).capitalize
    }

    private def initCap(parts: Seq[String]): String = {
      parts.map(s => initCap(s)).mkString("")
    }

  }
}

object RouteGenerator {

  def fromFile(path: String): RouteGenerator = {
    val json = Source.fromFile(path).mkString
    RouteGenerator(json)
  }

}
