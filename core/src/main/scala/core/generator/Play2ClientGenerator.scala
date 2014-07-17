package core.generator

import core._
import Text._
import ScalaUtil._

object Play2ClientGenerator {
  def apply(json: String): String = {
    Play2ClientGenerator(ServiceDescription(json))
  }

  def apply(sd: ServiceDescription): String = {
    val ssd = new ScalaServiceDescription(sd)
    apply(ssd)
  }

  def apply(ssd: ScalaServiceDescription): String = {
s"""${Play2Models(ssd)}

${client(ssd)}"""
  }

  private[generator] def errorTypeClass(response: ScalaResponse): String = {
    require(!response.isSuccess)

    Seq(
      s"case class ${response.errorClassName}(response: play.api.libs.ws.WSResponse) extends Exception {",
      "",
      s"  lazy val ${response.errorVariableName} = response.json.as[${response.resultType}]",
      "",
      "}"
    ).mkString("\n")
  }

  private[generator] def errors(ssd: ScalaServiceDescription): Option[String] = {
    val errorTypes = ssd.resources.flatMap(_.operations).flatMap(_.responses).filter(r => !(r.isSuccess || r.isUnit))

    if (errorTypes.isEmpty) {
      None
    } else {
      Some(
        Seq(
          "package error {",
          "",
          s"  import ${ssd.packageName}.models.json._",
          "",
          errorTypes.map { t => errorTypeClass(t) }.distinct.sorted.mkString("\n\n").indent(2),
          "}"
        ).mkString("\n").indent(2)
      )
    }
  }

  private def client(ssd: ScalaServiceDescription): String = {
    val errorsString = errors(ssd) match {
      case None => ""
      case Some(s: String) => s"\n\n$s\n"
    }

    val accessors = ssd.resources.map(_.model.plural).sorted.map { plural =>
      val methodName = Text.snakeToCamelCase(Text.camelCaseToUnderscore(plural).toLowerCase)
      s"def ${methodName} = ${plural}"
    }.mkString("\n\n")

s"""package ${ssd.packageName} {

  case class FailedResponse(response: play.api.libs.ws.WSResponse) extends Exception$errorsString

  class Client(apiUrl: String, apiToken: scala.Option[String] = None) {
    import ${ssd.packageName}.models._
    import ${ssd.packageName}.models.json._

    private val logger = play.api.Logger("${ssd.packageName}.client")

    logger.info(s"Initializing ${ssd.packageName}.client for url $$apiUrl")

${accessors.indent(4)}

    def _requestHolder(path: String): play.api.libs.ws.WSRequestHolder = {
      import play.api.Play.current

      val holder = play.api.libs.ws.WS.url(apiUrl + path)
      apiToken.fold(holder) { token =>
        holder.withAuth(token, "", play.api.libs.ws.WSAuthScheme.BASIC)
      }
    }

    def _logRequest(method: String, req: play.api.libs.ws.WSRequestHolder)(implicit ec: scala.concurrent.ExecutionContext): play.api.libs.ws.WSRequestHolder = {
      val queryComponents = for {
        (name, values) <- req.queryString
        value <- values
      } yield name -> value
      val url = s"$${req.url}$${queryComponents.mkString("?", "&", "")}"
      apiToken.fold(logger.info(s"curl -X $$method $$url")) { _ =>
        logger.info(s"curl -X $$method -u '[REDACTED]:' $$url")
      }
      req
    }

    private def POST(path: String, data: play.api.libs.json.JsValue = play.api.libs.json.Json.obj())(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("POST", _requestHolder(path)).post(data)
    }

    private def GET(path: String, q: Seq[(String, String)] = Seq.empty)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("GET", _requestHolder(path).withQueryString(q:_*)).get()
    }

    private def PUT(path: String, data: play.api.libs.json.JsValue = play.api.libs.json.Json.obj())(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("PUT", _requestHolder(path)).put(data)
    }

    private def PATCH(path: String, data: play.api.libs.json.JsValue = play.api.libs.json.Json.obj())(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("PATCH", _requestHolder(path)).patch(data)
    }

    private def DELETE(path: String)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("DELETE", _requestHolder(path)).delete()
    }

${modelClients(ssd).indent(2)}
  }
}"""
  }

  private def modelClients(ssd: ScalaServiceDescription): String = {
    ssd.resources.groupBy(_.model.plural).toSeq.sortBy(_._1).map { case (plural, resources) =>
      s"  trait $plural {\n" +
      clientMethods(ssd, resources).map(_.interface).mkString("\n\n").indent(4) +
      "\n  }\n\n" +
      s"  object $plural extends $plural {\n" +
      clientMethods(ssd, resources).map(_.code).mkString("\n\n").indent(4) +
      "\n  }"
    }.mkString("\n\n")
  }

  private def clientMethods(ssd: ScalaServiceDescription, resources: Seq[ScalaResource]): Seq[ClientMethod] = {
    resources.flatMap(_.operations).map { op =>
      val path = Play2Util.pathParams(op)

      val methodCall = if (Util.isJsonDocumentMethod(op.method)) {
        Play2Util.formParams(op) match {
          case None => s"${op.method}($path)"
          case Some(payload) => s"${payload}\n\n${op.method}($path, payload)"
        }

      } else {
        Play2Util.queryParams(op) match {
          case None => s"${op.method}($path)"
          case Some(query) => s"${query}\n\n${op.method}($path, query)"
        }
      }

      val hasOptionResult = op.responses.find { _.isOption } match {
        case None => ""
        case Some(r) => {
          if (r.isMultiple) {
            s"\ncase r if r.status == 404 => Nil"
          } else {
            s"\ncase r if r.status == 404 => None"
          }
        }
      }
      val matchResponse: String = {
        op.responses.flatMap { response =>
          if (response.isSuccess) {
            if (response.isOption) {
              if (response.isUnit) {
                Some(s"case r if r.status == ${response.code} => Some(Unit)")
              } else {
                Some(s"case r if r.status == ${response.code} => Some(r.json.as[${response.qualifiedScalaType}])")
              }

            } else if (response.isMultiple) {
              Some(s"case r if r.status == ${response.code} => r.json.as[scala.collection.Seq[${response.qualifiedScalaType}]]")

            } else if (response.isUnit) {
              Some(s"case r if r.status == ${response.code} => ${response.qualifiedScalaType}")

            } else {
              Some(s"case r if r.status == ${response.code} => r.json.as[${response.qualifiedScalaType}]")
            }

          } else if (response.isNotFound && response.isOption) {
            // will be added later
            None

          } else {
            Some(s"case r if r.status == ${response.code} => throw new ${ssd.packageName}.error.${response.errorClassName}(r)")
          }
        }.mkString("\n")
      } + hasOptionResult + "\ncase r => throw new FailedResponse(r)\n"

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
      s"""${commentString}def $name($argList.getOrElse(""))(implicit ec: scala.concurrent.ExecutionContext): $returnType"""
    }

    val code: String = {
      s"""override def $name($argList)(implicit ec: scala.concurrent.ExecutionContext): $returnType = {
${methodCall.indent}.map {
${response.indent(4)}
  }
}"""
    }
  }

}
