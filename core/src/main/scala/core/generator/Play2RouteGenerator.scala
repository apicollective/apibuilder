package core.generator

import core._
import io.Source

object Play2RouteGenerator {

  def apply(json: String): String = {
    generate(ServiceDescription(json))
  }

  def generate(sd: ServiceDescription): String = {
    new Play2RouteGenerator(sd).generate.getOrElse("")
  }
}


/**
 * Generates a Play routes file based on the service description
 * from api.json
 */
case class Play2RouteGenerator(service: ServiceDescription) {

  private val GlobalPad = 5

  def generate(): Option[String] = {
    val all = service.resources.flatMap { resource =>
      resource.operations.map { op =>
        Play2Route(op, resource)
      }
    }
    if (all.size == 0) {
      None
    } else {
      val maxVerbLength = all.map(_.verb.length).sorted.last
      val maxUrlLength = all.map(_.url.length).sorted.last
      val (paramStart, pathStart) = all.partition(_.url.startsWith("/:"))

      Some((pathStart ++ paramStart).map { r =>
        Seq(
          r.verb,
          " " * (maxVerbLength - r.verb.length + GlobalPad),
          r.url,
          " " * (maxUrlLength - r.url.length + GlobalPad),
          r.method,
          "(",
          r.params.mkString(", "),
          ")"
        ).mkString("")
      }.mkString("\n"))
    }
  }
}

private[generator] case class Play2Route(op: Operation, resource: Resource) {

  lazy val verb = op.method
  lazy val url = op.path
  lazy val method = s"$controllerName.$methodName"

  lazy val params = if (Util.isJsonDocumentMethod(verb)) {
    pathParameters
  } else {
    parameters
  }

  private lazy val parameters = parametersWithTypes(op.parameters)
  private lazy val pathParameters = parametersWithTypes(op.pathParameters)

  private lazy val methodName = GeneratorUtil.urlToMethodName(resource.path, op.method, url)

  private lazy val controllerName: String = "controllers." + Text.underscoreToInitCap(op.model.plural)

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

      case et: EnumParameterType => {
        // TODO: Should we use the real class here or leave to user to convert?
        if (param.required) {
          "String"
        } else {
          s"Option[String]"
        }
      }

      case dt: PrimitiveParameterType => {
        val scalaType = dt.datatype match {
          case Datatype.StringType => "String"
          case Datatype.DoubleType => "Double"
          case Datatype.LongType => "Long"
          case Datatype.IntegerType => "Int"
          case Datatype.BooleanType => "Boolean"
          case Datatype.DecimalType => "BigDecimal"
          case Datatype.UuidType => "java.util.UUID"
          case Datatype.DateTimeIso8601Type => "org.joda.time.DateTime"
          case Datatype.UnitType => "Unit"
          case Datatype.MapType => sys.error("Map not allowed as a parameter")
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

