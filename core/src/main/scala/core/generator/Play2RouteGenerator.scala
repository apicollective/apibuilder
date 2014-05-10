package core.generator

import core.{ Datatype, Field, Operation, Model, ModelParameterType, Parameter, ParameterLocation, PrimitiveParameterType, ServiceDescription, Text }
import io.Source

/**
 * Generates a Play routes file based on the service description
 * from api.json
 */
case class Play2RouteGenerator(service: ServiceDescription) {

  private val GlobalPad = 5

  def generate(): Option[String] = {
    val all = service.operations.map { Route(_) }
    if (all.size == 0) {
      None
    } else {
      val maxVerbLength = all.map(_.verb.length).sorted.last
      val maxUrlLength = all.map(_.url.length).sorted.last

      Some(all.map { r =>
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
      }.mkString("\n"))
    }
  }

  private[this] case class Route(op: Operation) {

    lazy val verb = op.method
    lazy val url = service.basePath.getOrElse("") + op.path
    lazy val method = s"$controllerName.$methodName"

    lazy val parameters = parametersWithTypes(op.parameters)
    lazy val pathParameters = parametersWithTypes(op.parameters.filter { param => param.location == ParameterLocation.Path })

    private lazy val methodName = op.method.toLowerCase + Text.initCap(op.path.split("/"))

    private lazy val controllerName: String = "controllers." + Text.underscoreToInitCap(op.model.name)

    private def parametersWithTypes(params: Seq[Parameter]): Seq[String] = {
      params.map { param =>
        Seq(
          Some(s"${param.name}: ${scalaDataType(param)}"),
          param.default.map( d => s"?= ${d}" )
        ).flatten.mkString(" ")
      }
    }

    private def scalaDataType(param: Parameter): String = {
      param.paramtype match {

        case dt: ModelParameterType => {
          sys.error("Model parameter types not supported in play routes")
        }

        case dt: PrimitiveParameterType => {
          val scalaType = dt.datatype match {
            case Datatype.String => "String"
            case Datatype.Long => "Long"
            case Datatype.Integer => "Int"
            case Datatype.Boolean => "Boolean"
            case Datatype.Decimal => "BigDecimal"
            case Datatype.Uuid => "UUID"
            case Datatype.DateTimeIso8601 => "DateTime"
            case Datatype.UnitDatatype => "Unit"
          }

          if (param.required) {
            scalaType
          } else {
            s"Option[$scalaType]"
          }
        }
      }
    }
  }
}
