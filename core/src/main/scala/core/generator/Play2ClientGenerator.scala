package core.generator

import core._
import Text._
import ScalaUtil._

object Play2ClientGenerator {
  def apply(json: String): String = {
    val sd = ServiceDescription(json)
    val ssd = new ScalaServiceDescription(sd)
    apply(ssd)
  }

  def apply(ssd: ScalaServiceDescription): String = {
s"""${Play2Models(ssd)}

${client(ssd)}"""
  }

  private def client(ssd: ScalaServiceDescription): String = {
s"""package ${ssd.packageName} {

  case class FailedResponse(response: play.api.libs.ws.Response) extends Exception

  case class ErrorResponse(response: play.api.libs.ws.Response) extends Exception {

    case class Error (
      code: String,
      message: String
    )

    object Error {
      implicit def readsError: play.api.libs.json.Reads[Error] = {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \\ "code").read[String] and
          (__ \\ "message").read[String])(Error.apply _)
      }
    
      implicit def writesError: play.api.libs.json.Writes[Error] = {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \\ "code").write[String] and
          (__ \\ "message").write[String])(unlift(Error.unapply))
      }
    }

    val errors: Seq[Error] = response.json.as[scala.collection.Seq[Error]]
  }

  class Client(apiUrl: String, apiToken: Option[String] = None) {
    import ${ssd.packageName}.models._
    import ${ssd.packageName}.models.json._

    private val logger = play.api.Logger("${ssd.packageName}.client")

    logger.info(s"Initializing ${ssd.packageName}.client for url $$apiUrl")

    def requestHolder(path: String): play.api.libs.ws.WSRequestHolder = {
      import play.api.Play.current

      val holder = play.api.libs.ws.WS.url(apiUrl + path)
      apiToken match {
        case None => holder
        case Some(token: String) => {
          holder.withAuth(token, "", play.api.libs.ws.WSAuthScheme.BASIC)
        }
      }
    }

    def logRequest(method: String, req: play.api.libs.ws.WSRequestHolder)(implicit ec: scala.concurrent.ExecutionContext): play.api.libs.ws.WSRequestHolder = {
      val q = req.queryString.flatMap { case (name, values) =>
        values.map(name -> _).map { case (name, value) =>
          s"$$name=$$value"
        }
      }.mkString("&")
      val url = s"$${req.url}?$$q"
      apiToken.map { _ =>
        logger.info(s"curl -X $$method -u '[REDACTED]:' $$url")
      }.getOrElse {
        logger.info(s"curl -X $$method $$url")
      }
      req
    }

    private def POST(path: String, data: play.api.libs.json.JsValue)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      logRequest("POST", requestHolder(path)).post(data)
    }

    private def GET(path: String, q: Seq[(String, String)])(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      logRequest("GET", requestHolder(path).withQueryString(q:_*)).get()
    }

    private def PUT(path: String, data: play.api.libs.json.JsValue)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      logRequest("PUT", requestHolder(path)).put(data)
    }

    private def PATCH(path: String, data: play.api.libs.json.JsValue)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      logRequest("PATCH", requestHolder(path)).patch(data)
    }

    private def DELETE(path: String)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      logRequest("DELETE", requestHolder(path)).delete()
    }

${modelClients(ssd).indent(4)}
  }
}"""
  }

  private def modelClients(ssd: ScalaServiceDescription): String = {
    val grouped = ssd.resources.groupBy(_.model.plural)
    grouped.toSeq.sortBy(_._1).map { case (plural, resources) =>
      s"  trait $plural {\n" +
      clientMethods(resources).map(_.interface).mkString("\n\n").indent(4) +
      "  }\n\n" +
      s"  object $plural extends $plural {\n" +
      clientMethods(resources).map(_.code).mkString("\n\n").indent(4) +
      "  }"
    }.mkString("\n\n")
  }

  private def clientMethods(resources: Seq[ScalaResource]): Seq[ClientMethod] = {
    resources.flatMap(_.operations).map { op =>
      val path: String = Play2Util.pathParams(op)
      def payload = Play2Util.formParams(op)
      val methodCall: String = op.method match {
        case "GET" => {
          s"""${Play2Util.queryParams(op)}

GET($path, queryBuilder.result)"""
        }

        case "POST" => {
          s"""$payload

POST($path, payload)"""
        }

        case "PUT" => {
          s"""$payload

PUT($path, payload)"""
        }

        case "PATCH" => {
          s"""$payload

PATCH($path, payload)"""
        }

        case "DELETE" => s"DELETE($path)"

        case _ => throw new UnsupportedOperationException(s"Attempt to use operation with unsupported type: ${op.method}")
      }

      // TODO: Inject a 404 response if this operation returns an option
      val hasOptionResult = op.responses.find { _.isOption } match {
        case None => ""
        case Some(r) => s"\ncase r if r.status == 404 => None"
      }
      val matchResponse: String = {
        op.responses.map { response =>
          if (response.isSuccess) {
            if (response.isOption) {
              s"case r if r.status == ${response.code} => Some(r.json.as[${response.scalaType}])"

            } else if (response.isMultiple) {
              s"case r if r.status == ${response.code} => r.json.as[Seq[${response.scalaType}]]"

            } else {
              s"case r if r.status == ${response.code} => r.json.as[${response.scalaType}]"
            }

          } else if (response.isNotFound && response.isOption) {
            // will be added later
          } else {
            s"case r if r.status == ${response.code} => throw new ErrorResponse(r)"
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
    argList: String,
    returnType: String,
    methodCall: String,
    response: String,
    comments: Option[String]
  ) {
    import Text._
    
    private val commentString = comments.map(string => ScalaUtil.textToComment(string) + "\n").getOrElse("")

    val interface: String = {
      s"""${commentString}def $name($argList)(implicit ec: scala.concurrent.ExecutionContext): $returnType"""
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
