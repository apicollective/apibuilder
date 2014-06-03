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
s"""${client(ssd)}

${Play2Models(ssd)}"""
  }

  private def client(ssd: ScalaServiceDescription): String = {
    def packageName = ssd.name.toLowerCase
s"""package play.api.libs.apidoc.$packageName {
  /**
   * A helper that provides access to some needed, but private
   * functionality of the play WS client library.
   */
  object WSHelper {
    /**
     * Allows users to perform patch requests using a WSRequestHolder.
     * Necessary in play 2.2.x, but needed for 2.3 +.
     */
    def patch(
      req: play.api.libs.ws.WS.WSRequestHolder,
      data: play.api.libs.json.JsValue
    ): scala.concurrent.Future[play.api.libs.ws.Response] = {
      req.prepare("PATCH", data).execute
    }
  }
}

package $packageName {
  class Client(apiUrl: String, apiToken: Option[String] = None) {
    import $packageName.models._

    private val logger = play.api.Logger("$packageName.client")

    logger.info(s"Initializing $packageName.client for url $$apiUrl")

    private def requestHolder(resource: String) = {
      val url = apiUrl + resource
      val holder = play.api.libs.ws.WS.url(url)
      apiToken.map { token =>
        holder.withAuth(token, "", com.ning.http.client.Realm.AuthScheme.BASIC)
      }.getOrElse {
        holder
      }
    }

    private def logRequest(method: String, req: play.api.libs.ws.WS.WSRequestHolder)(implicit ec: scala.concurrent.ExecutionContext): play.api.libs.ws.WS.WSRequestHolder = {
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

    private def processResponse(f: scala.concurrent.Future[play.api.libs.ws.Response])(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.Response] = {
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

    private def POST(path: String, data: play.api.libs.json.JsValue)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.Response] = {
      processResponse(logRequest("POST", requestHolder(path)).post(data))
    }

    private def GET(path: String, q: Seq[(String, String)])(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.Response] = {
      processResponse(logRequest("GET", requestHolder(path).withQueryString(q:_*)).get())
    }

    private def PUT(path: String, data: play.api.libs.json.JsValue)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.Response] = {
      processResponse(logRequest("PUT", requestHolder(path)).put(data))
    }

    private def PATCH(path: String, data: play.api.libs.json.JsValue)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.Response] = {
      processResponse(play.api.libs.apidoc.$packageName.WSHelper.patch(logRequest("PATCH", requestHolder(path)), data))
    }

    private def DELETE(path: String)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.Response] = {
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
          if (tpe == "Unit") {
            s"case r if r.status == ${response.code} => r.status -> ()"
          } else {
            s"""case r if r.status == ${response.code} => {
  try r.status -> r.json.as[$tpe]
  catch {
    case e: Exception => throw new RuntimeException(s"unable to parse '$tpe' from $${r.json}", e)
  }
}"""
          }
        }.mkString("\n\n")
      }
      s"""${op.scaladoc}def ${op.name}(${op.argList})(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Any] = {
${methodCall.indent}.map {
${matchResponse.indent(4)}

    case r => r
  }
}"""
    }.mkString("\n\n")
  }
}
