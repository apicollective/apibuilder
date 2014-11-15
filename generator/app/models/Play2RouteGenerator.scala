package models

import lib.Primitives
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
  val params = parametersWithTypesAndDefaults(op.parameters.filter(_.location != ParameterLocation.Form).filter(_.`type`.container == Container.Singleton))

  /**
    * Play does not have native support for providing a list as a
    * query parameter. Document these query parameters in the routes
    * file - but do not implement.
    */
  val paramComments: Option[String] = op.parameters.filter(_.location != ParameterLocation.Form).filter(_.`type`.container != Container.Singleton) match {
    case Nil => None
    case paramsToComment => {
      Some(
        Seq(
          s"# Additional parameters to ${op.method} ${op.path}",
          paramsToComment.map { p =>
            "#   - " + parameterWithType(ssd, p)
          }.mkString("\n")
        ).mkString("\n")
      )
    }
  }

  val method = "%s.%s".format(
    "controllers." +lib.Text.underscoreAndDashToInitCap(op.model.plural),
    GeneratorUtil.urlToMethodName(resource.model.plural, resource.path, op.method, url)
  )

  private def parametersWithTypesAndDefaults(params: Seq[Parameter]): Seq[String] = {
    params.map { param =>
      Seq(
        Some(parameterWithType(ssd, param)),
        param.default.map( d =>
          param.`type` match {
            case TypeInstance(Container.List, _) => {
              sys.error("Cannot set defaults for lists")
            }
            case TypeInstance(Container.Map, _) => {
              sys.error("Cannot set defaults for maps")
            }
            case TypeInstance(Container.Singleton, Type(TypeKind.Primitive, name)) => {
              Primitives(name) match {
                case None => {
                  sys.error("Unknown primitive type[$name]")
                }
                case Some(pt) => pt match {
                  case Primitives.String | Primitives.DateIso8601 | Primitives.DateTimeIso8601 | Primitives.Uuid => {
                    s"""?= "$d""""
                  }
                  case Primitives.Integer | Primitives.Double | Primitives.Long | Primitives.Boolean | Primitives.Decimal => {
                    s"?= ${d}"
                  }
                  case Primitives.Unit => {
                    sys.error(s"Unsupported type[$pt] for default values")
                  }
                }
              }
            }
            case TypeInstance(Container.Singleton, Type(TypeKind.Model, name)) => {
              sys.error(s"Models cannot be defaults in path parameters")
            }
            case TypeInstance(Container.Singleton, Type(TypeKind.Enum, name)) => {
              s"""?= "${d}""""
            }
            case TypeInstance(Container.UNDEFINED(container), _) => {
              sys.error(s"Invalid container[$container]")
            }
            case TypeInstance(_, Type(TypeKind.UNDEFINED(kind), name)) => {
              sys.error(s"Invalid typeKind[$kind] for name[$name]")
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
    val datatype = ssd.scalaDataType(param.`type`)
    param.`type`.container match {
      case Container.Singleton | Container.List | Container.Map => {
        if (param.required) {
          datatype.name
        } else {
          s"scala.Option[${datatype.name}]"
        }
      }

      case Container.UNDEFINED(container) => {
        sys.error(s"Invalid container[$container]")
      }

    }
  }

}

