package core.generator

import core.{ Datatype, Field, Operation, Resource, ServiceDescription, Text }
import io.Source

/**
 * Generates a Play routes file based on the service description
 * from api.json
 */
case class Play2RouteGenerator(service: ServiceDescription) {

  private val GlobalPad = 5

  def generate(): String = {
    val all = service.resources.flatMap { resource =>
      resource.operations.map { op =>
        Route(resource, op)
      }
    }
    val maxVerbLength = all.map(_.verb.length).sorted.last
    val maxUrlLength = all.map(_.url.length).sorted.last

    all.map { r =>
      val params = if (GeneratorUtil.isJsonDocumentMethod(r.verb)) {
        r.pathParameters
      } else {
        r.parameters
      }

      Seq(
        r.verb,
        " " * (maxVerbLength - r.verb.length + GlobalPad),
        r.url,
        " " * (maxUrlLength - r.url.length + GlobalPad),
        r.method,
        "(",
        params.mkString(", "),
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
    lazy val parameters = parametersWithTypes(op.parameters)
    lazy val pathParameters = parametersWithTypes(op.parameters.filter { param => namedParametersInPath.contains(param.name) })

    private lazy val namedParametersInPath = GeneratorUtil.namedParametersInPath(resource.path + op.path.getOrElse(""))

    private lazy val methodName = {
      op.path match {
        case None => {
          op.method.toLowerCase
        }

        case Some(path: String) => {
          op.method.toLowerCase + Text.initCap(path.split("/"))
        }
      }
    }

    private lazy val controllerName: String = {
      s"controllers.${Text.underscoreToInitCap(resource.name)}"
    }

    private def parametersWithTypes(params: Seq[Field]): Seq[String] = {
      params.map { param =>
        Seq(
          Some(s"${param.name}: ${scalaDataType(param)}"),
          param.default.map( d => s"?= ${d}" )
        ).flatten.mkString(" ")
      }
    }

    private def scalaDataType(param: Field): String = {
      val scalaType = param.datatype match {
        case Datatype.String => "String"
        case Datatype.Long => "Long"
        case Datatype.Integer => "Int"
        case Datatype.Boolean => "Boolean"
        case Datatype.Decimal => "BigDecimal"
        case Datatype.Unit => "Unit"
        case _ => {
          sys.error(s"Cannot map data type[${param.datatype}] to scala type")
        }
      }

      if (param.required) {
        scalaType
      } else {
        s"Option[$scalaType]"
      }
    }
  }
}
