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
  trait Response[T] {
    val entity: T
    val status: Int
  }

  object Response {
    def unapply[T](r: Response[T]) = Some((r.entity, r.status))
  }

  case class ResponseImpl[T](entity: T, status: Int) extends Response[T]

  case class FailedResponse[T](entity: T, status: Int)
    extends Exception(s"request failed with status[$$status]: $${entity}")
    with Response[T]

  class Client(apiUrl: String, apiToken: Option[String] = None) {
    import ${ssd.packageName}.models._
    import ${ssd.packageName}.models.json._

    private val logger = play.api.Logger("${ssd.packageName}.client")

    logger.info(s"Initializing ${ssd.packageName}.client for url $$apiUrl")

    def requestHolder(path: String): play.api.libs.ws.WSRequestHolder = {
      import play.api.Play.current

      val url = apiUrl + path
      val holder = play.api.libs.ws.WS.url(url)
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

    def processResponse(f: scala.concurrent.Future[play.api.libs.ws.WSResponse])(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      f.map { response =>
        lazy val body: String = scala.util.Try {
          play.api.libs.json.Json.prettyPrint(response.json)
        } getOrElse {
          response.body
        }
        logger.debug(s"$${response.status} -> $$body")
        response
      }
    }

    private def POST(path: String, data: play.api.libs.json.JsValue)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      processResponse(logRequest("POST", requestHolder(path)).post(data))
    }

    private def GET(path: String, q: Seq[(String, String)])(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      processResponse(logRequest("GET", requestHolder(path).withQueryString(q:_*)).get())
    }

    private def PUT(path: String, data: play.api.libs.json.JsValue)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      processResponse(logRequest("PUT", requestHolder(path)).put(data))
    }

    private def PATCH(path: String, data: play.api.libs.json.JsValue)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      processResponse(logRequest("PATCH", requestHolder(path)).patch(data))
    }

    private def DELETE(path: String)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      processResponse(logRequest("DELETE", requestHolder(path)).delete())
    }

${modelClients(ssd).indent(4)}
  }
}"""
  }

  private def modelClients(ssd: ScalaServiceDescription): String = {
    val grouped = ssd.resources.groupBy(_.model.plural)
    grouped.toSeq.sortBy(_._1).map { case (plural, resources) =>
      s"""object $plural {
${clientMethods(resources).indent}
}"""
    }.mkString("\n\n")
  }

  private def clientMethods(resources: Seq[ScalaResource]): String = {
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
      val matchResponse: String = {
        op.responses.map { response =>
          val tpe = response.datatype
          val code = response.code
          val entity = if (tpe == "Unit") "()" else s"r.json.as[$tpe]"
          val action = if (response.isSuccess) {
            "new ResponseImpl"
          } else {
            s"throw new FailedResponse"
          }
          s"case r if r.status == $code => $action($entity, $code)"
        }.mkString("\n")
      }
      val comments = op.description.map(desc => ScalaUtil.textToComment(desc) + "\n").getOrElse("")
      val returnType = s"scala.concurrent.Future[Response[${op.resultType}]]"
      s"""${comments}def ${op.name}(${op.argList})(implicit ec: scala.concurrent.ExecutionContext): $returnType = {
${methodCall.indent}.map {
${matchResponse.indent(4)}
    case r => throw new FailedResponse(r.body, r.status)
  }
}"""
    }.mkString("\n\n")
  }
}
