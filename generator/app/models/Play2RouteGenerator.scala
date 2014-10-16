package models

import com.gilt.apidocgenerator.models._
import core._
import core.generator.{ScalaDataType, GeneratorUtil, ScalaServiceDescription, CodeGenerator}

object Play2RouteGenerator extends CodeGenerator {

  def apply(json: String): String = {
    generate(ServiceDescriptionBuilder(json))
  }

  def apply(sd: ServiceDescription): Play2RouteGenerator = {
    Play2RouteGenerator(new ScalaServiceDescription(sd))
  }

  override def generate(sd: ServiceDescription): String = {
    val ssd = new ScalaServiceDescription(sd)
    generate(ssd)
  }

  def generate(ssd: ScalaServiceDescription): String = {
    new Play2RouteGenerator(ssd).generate.getOrElse("")
  }
}


/**
 * Generates a Play routes file based on the service description
 * from api.json
 */
case class Play2RouteGenerator(scalaService: ScalaServiceDescription) {

  private val GlobalPad = 5

  private val service = scalaService.serviceDescription

  def generate(): Option[String] = {
    val all = service.resources.flatMap { resource =>
      resource.operations.map { op =>
        Play2Route(scalaService, op, resource)
      }
    }
    if (all.isEmpty) {
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
          ")",
          r.paramComments.map( c => "\n" + c ).getOrElse("")
        ).mkString("")
      }.mkString("\n"))
    }
  }
}

private[models] case class Play2Route(ssd: ScalaServiceDescription, op: Operation, resource: Resource) {

  val verb = op.method
  val url = op.path
  val params = parametersWithTypesAndDefaults(op.parameters.filter(!_.datatype.multiple).filter(_.location != ParameterLocation.Form))

  /**
    * Play does not have native support for providing a list as a
    * query parameter. Document these query parameters in the routes
    * file - but do not implement.
    */
  private val parametersToComment = op.parameters.filter(_.datatype.multiple).filter(_.location != ParameterLocation.Form)
  val paramComments = if (parametersToComment.isEmpty) {
    None
  } else {
    Some(
      Seq(
        s"# Additional parameters to ${op.method} ${op.path}",
        parametersToComment.map { p =>
          "#   - " + parameterWithType(ssd, p)
        }.mkString("\n")
      ).mkString("\n")
    )
  }

  val method = "%s.%s".format(
    "controllers." + Text.underscoreAndDashToInitCap(op.model.plural),
    GeneratorUtil.urlToMethodName(resource.model.plural, resource.path, op.method, url)
  )

  private def parametersWithTypesAndDefaults(params: Seq[Parameter]): Seq[String] = {
    params.map { param =>
      Seq(
        Some(parameterWithType(ssd, param)),
        param.default.map( d =>
          param.datatype match {
            case Type(TypeKind.Primitive, name, _) =>
              val datatype = Datatype.forceByName(name)
              datatype match {
                case Datatype.StringType | Datatype.UnitType | Datatype.DateIso8601Type | Datatype.DateTimeIso8601Type | Datatype.UuidType => {
                  s"""?= "$d""""
                }
                case Datatype.IntegerType | Datatype.DoubleType | Datatype.LongType | Datatype.BooleanType | Datatype.DecimalType => {
                  s"?= ${d}"
                }
                case Datatype.UnitType | Datatype.MapType => {
                  sys.error(s"Unsupported type[${datatype}] for default values")
                }
            }
            case Type(TypeKind.Model, _, _) => {
              sys.error(s"Models cannot be defaults in path parameters")
            }
            case Type(TypeKind.Enum, _, _) => {
              s"""?= "${d}""""
            }
          }
        )
      ).flatten.mkString(" ")
    }
  }

  private def parameterWithType(ssd: ScalaServiceDescription, param: Parameter): String = {
    s"${param.name}: ${scalaDataType(ssd, param)}"
  }

  private def scalaDataType(ssd: ScalaServiceDescription, param: Parameter): String = {
    param.datatype match {
      case Type(TypeKind.Model, _, _) => {
        sys.error("Model parameter types not supported in play routes")
      }
      case Type(TypeKind.Enum, enumName, _) => {
        qualifyParam(ssd.enumClassName(enumName), param.required, param.datatype.multiple)
      }
      case Type(TypeKind.Primitive, n, _) => {
        val dt = Datatype.forceByName(n)
        val name = ScalaDataType(dt).name
        qualifyParam(name, param.required, param.datatype.multiple)
      }
    }
  }

  private def qualifyParam(name: String, required: Boolean, multiple: Boolean): String = {
    if (!required && multiple) {
      s"Option[Seq[$name]]"
    } else if (!required) {
      s"Option[$name]"
    } else if (multiple) {
      s"Seq[$name]"
    } else {
      name
    }
  }

}

