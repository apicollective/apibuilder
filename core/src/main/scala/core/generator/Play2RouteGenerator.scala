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

private[generator] case class Play2Route(op: Operation, resource: Resource) {

  val verb = op.method
  val url = op.path
  val params = parametersWithTypesAndDefaults(op.parameters.filter(!_.multiple).filter(_.location != ParameterLocation.Form))

  /**
    * Play does not have native support for providing a list as a
    * query parameter. Document these query parameters in the routes
    * file - but do not implement.
    */
  private val parametersToComment = op.parameters.filter(_.multiple).filter(_.location != ParameterLocation.Form)
  val paramComments = if (parametersToComment.isEmpty) {
    None
  } else {
    Some(
      Seq(
        "# Additional parameters to GET /echos",
        parametersToComment.map { p =>
          "#   - " + parameterWithType(p)
        }.mkString("\n")
      ).mkString("\n")
    )
  }

  val method = "%s.%s".format(
    "controllers." + Text.underscoreToInitCap(op.model.plural),
    GeneratorUtil.urlToMethodName(resource.path, op.method, url)
  )

  private def parametersWithTypesAndDefaults(params: Seq[Parameter]): Seq[String] = {
    params.map { param =>
      Seq(
        Some(parameterWithType(param)),
        param.default.map( d => s"?= ${d}" )
      ).flatten.mkString(" ")
    }
  }

  private def parameterWithType(param: Parameter): String = {
    s"${param.name}: ${scalaDataType(param)}"
  }

  private def scalaDataType(param: Parameter): String = {
    assert(!param.multiple)

    param.paramtype match {
      case dt: ModelParameterType => {
        sys.error("Model parameter types not supported in play routes")
      }
      case et: EnumParameterType => {
        // TODO: Should we use the real class here or leave to user to convert?
        qualifyParam(ScalaDataType.ScalaStringType.name, param.required)
      }
      case dt: PrimitiveParameterType => {
        val name = ScalaDataType(dt.datatype).name
        qualifyParam(name, param.required)
      }
    }
  }

  private def qualifyParam(name: String, required: Boolean): String = {
    if (!required) {
      s"Option[$name]"
    } else {
      name
    }
  }

}

