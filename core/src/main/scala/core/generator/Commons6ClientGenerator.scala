package core.generator

import core._
import Text._
import ScalaUtil._

object Commons6ClientGenerator {

  def generate(sd: ServiceDescription, userAgent: String): String = {
    val ssd = new ScalaServiceDescription(sd)
    generate(ssd, userAgent)
  }

  def generate(ssd: ScalaServiceDescription, userAgent: String): String = {
    Commons6ClientGenerator(ssd, userAgent).generate()
  }

}

case class Commons6ClientGenerator(ssd: ScalaServiceDescription, userAgent: String) {

  def generate(): String = {
    Seq(
      ScalaCaseClasses.generate(ssd, Commons6),
      client()
    ).mkString("\n\n")
  }

  private[generator] def errorTypeClass(response: ScalaResponse): String = {
    require(!response.isSuccess)

    val responseType = if (response.isOption) {
      // In the case of errors, ignore the option wrapper as we only
      // trigger the error response when we have an actual error.
      response.qualifiedScalaType
    } else {
      response.resultType
    }

    // pass in status and UNPARSED body so that there is still a useful error
    // message even when the body is malformed and cannot be parsed
    Seq(
      s"""case class ${response.errorClassName}(response: Response) extends FailedRequest("Validation error", response) {""",
      s"lazy val ${response.errorVariableName} = CommonsJson.parse(response.body)".indent(2),
      "}"
    ).mkString("\n")
  }

  private[generator] def errors(): Option[String] = {
    val errorTypes = ssd.resources.flatMap(_.operations).flatMap(_.responses).filter(r => !(r.isSuccess || r.isUnit))

    if (errorTypes.isEmpty) {
      None
    } else {
      Some(
        Seq(
          "package error {",
          "",
          errorTypes.map { t => errorTypeClass(t) }.distinct.sorted.mkString("\n\n").indent(2),
          "}"
        ).mkString("\n").indent(2)
      )
    }
  }

  case class Header(name: String, value: String)

  private def headers(): Seq[Header] = {
    ssd.serviceDescription.headers.filter(!_.default.isEmpty).map { h =>
      Header(h.name, s""""${h.default.get}"""")
    } ++ Seq(Header("User-Agent", "UserAgent"), Header("Content-Type", """"application/vnd.event.gilt.v1+json""""))
  }

  private def client(): String = {
    val methodGenerator = ScalaClientMethodGenerator(ScalaClientMethodConfigs.Commons, ssd)

    val clientName = Text.pluralize(ssd.name)

    val patchMethod = s"""sys.error("PATCH method is not supported in Commons)"""

    val headerString = "private lazy val Headers = Map(" +
    headers.map { h =>
      s""""${h.name}" -> ${h.value}"""
    }.mkString(", ") + ").mapValues(Seq(_))"

    s"""package ${ssd.packageName} {
  import scala.concurrent.{Future, ExecutionContext}
  import com.gilt.commons.client._
  import com.gilt.commons.json.CommonsJson
  import com.gilt.commons.util.config.{Configuration, CommonsConfiguration}
  import ${ssd.packageName}.models._

  trait ${clientName} {

${methodGenerator.accessors().indent(4)}

${methodGenerator.traits().indent(4)}

  }

  private[${ssd.packageNamePrivate}] trait ${clientName}Client extends SimpleCommonsStyleClient with ${clientName} with ServiceIsReadWrite {

    def baseUri: String

    private val UserAgent = "$userAgent"

${headerString.indent(4)}

${modelClientImpl().indent(4)}
  }


  object ${clientName}Factory {
    lazy val instance = (new Configurable${clientName}Factory with CommonsConfiguration).instance
  }

  private[${ssd.packageNamePrivate}] trait Configurable${clientName}Factory {
    self: Configuration =>

    def instance: ${clientName} = {
      new ${clientName}Client {
        override val clientConfigSectionName = "${ssd.packageNamePrivate}"
        override val baseConfig = self.config
        override val clientName = "${ssd.packageNamePrivate}-client"
        override val baseUri = config.getRequiredString("${ssd.packageNamePrivate}_uri")
      }
    }
  }

${methodGenerator.errorPackage().indent(2)}
}"""
  }

  private def modelClientImpl(): String = {
    ssd.resources.groupBy(_.model.plural).toSeq.sortBy(_._1).map { case (plural, resources) =>
      val methodName = Text.snakeToCamelCase(Text.camelCaseToUnderscore(plural).toLowerCase)
      s"override val ${methodName} = new $plural {\n" +
      clientMethods(resources).map(_.code).mkString("\n\n").indent(2) +
      "\n}"
    }.mkString("\n\n")
  }

  private def formBody(op: ScalaOperation): Option[String] = {
    // Can have both or form params but not both as we can only send a single document
    assert(op.body.isEmpty || op.formParameters.isEmpty)

    if (op.formParameters.isEmpty && op.body.isEmpty) {
      None

    } else if (!op.body.isEmpty) {
      val payload = op.body.get.body match {
        case PrimitiveBody(dt, multiple) => ScalaDataType.asString(ScalaUtil.toVariable(op.body.get.name, multiple), ScalaDataType(dt))
        case ModelBody(name, multiple) => ScalaUtil.toVariable(op.body.get.name, multiple)
        case EnumBody(name, multiple) => s"${ScalaUtil.toVariable(op.body.get.name, multiple)}.map(_.toString)"
      }

      Some(s"val payload = RequestBody.fromJsonString(CommonsJson.generate($payload))")

    } else {
      val params = op.formParameters.map { param =>
        if (param.isOption) {
          s"""${param.name}.map("${param.originalName}" -> _)""".trim
        } else {
          s"""Some("${param.originalName}" -> ${param.name})""".trim
        }
      }.mkString(",\n")
      Some(
        Seq(
          "val data: Map[String, Any] = Seq(",
          params.indent,
          ").flatten.toMap",
          "val payload = RequestBody.fromJsonString(CommonsJson.generate(data))"
        ).mkString("\n")
      )
    }
  }

  private def clientMethods(resources: Seq[ScalaResource]): Seq[ClientMethod] = {
    resources.flatMap(_.operations).map { op =>
      val play2Util = Play2Util(ScalaClientMethodConfigs.Commons)
      val path = play2Util.pathParams(op)
      val query = play2Util.params("queryParameters", op.queryParameters)
      val queryVar = query.fold("Seq.empty")(_ => "queryParameters")
      val payload = formBody(op)
      val payloadVar = payload.fold("None")(_ => "Some(payload)")

      val methodCall = Seq(
        query,
        payload,
        Some(s"""getFullResponse(baseUri, ${path}, ${op.method}, Headers, ${payloadVar}, ${queryVar})""")
      ).flatten.mkString("\n\n")

      val hasOptionResult = op.responses.filter(_.isSuccess).find(_.isOption) match {
        case None => ""
        case Some(r) => {
          if (r.isMultiple) {
            s"\ncase 404 => Nil"
          } else {
            s"\ncase 404 => None"
          }
        }
      }

      val matchResponse: String = {
        op.responses.flatMap { response =>
          if (response.isSuccess) {
            if (response.isOption) {
              if (response.isUnit) {
                Some(s"case ${response.code} => Some(Unit)")
              } else {
                Some(s"case ${response.code} => Some(CommonsJson.parse(response.body))")
              }

            } else if (response.isUnit) {
              Some(s"case ${response.code} => Unit")

            } else {
              Some(s"case ${response.code} => CommonsJson.parse(response.body)")
            }

          } else if (response.isNotFound && response.isOption) {
            // will be added later
            None

          } else {
            Some(s"case ${response.code} => throw new ${ssd.packageName}.error.${response.errorClassName}(response)")
          }
        }.mkString("\n")
      } + s"""${hasOptionResult}
             |case _ => throw new FailedRequest(\"Unknown error\", response)
             |""".stripMargin

      ClientMethod(
        name = op.name,
        argList = op.argList,
        returnType = s"scala.concurrent.Future[${op.resultType}]",
        methodCall = methodCall,
        response = matchResponse,
        comments = op.description
      )

    }
  }


  private case class ClientMethod(
    name: String,
    argList: Option[String],
    returnType: String,
    methodCall: String,
    response: String,
    comments: Option[String]
  ) {
    import Text._
    
    private val commentString = comments.map(string => ScalaUtil.textToComment(string) + "\n").getOrElse("")

    val interface: String = {
      s"""${commentString}def $name(${argList.getOrElse("")})(implicit ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global): $returnType"""
    }

    val code: String = {
      s"""override def $name(${argList.getOrElse("")})(implicit ec: scala.concurrent.ExecutionContext): $returnType = {
${methodCall.indent} { response: Response =>
    response.statusCode match {
${response.indent(6)}
    }
  }
}"""
    }
  }

}
