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
s"""package play.api.libs {
  // TODO remove when we no longer support 2.2.x
  // dirty trick to make it easy to use PATCH:
  //   - WSRequestHolder.prepare is package private to play...
  //   - WSRequestHolder.execute is package private to libs...
  object ApidocWSHelper {
    def patch(
      req: play.api.libs.ws.WS.WSRequestHolder,
      data: play.api.libs.json.JsValue
    ): scala.concurrent.Future[play.api.libs.ws.Response] = {
      req.prepare("PATCH", data).execute
    }
  }
}

package $packageName {
  object Client {
    import $packageName.models._

    private val apiToken = sys.props.getOrElse(
      "$packageName.api.token",
      sys.error("API token must be provided")
    )

    private val apiUrl = sys.props.getOrElse(
      "$packageName.api.url",
      sys.error("API URL must be provided")
    )

    private val logger = play.api.Logger("$packageName.client")

    private def requestHolder(resource: String) = {
      val url = apiUrl + resource
      play.api.libs.ws.WS.url(url).withAuth(apiToken, "", com.ning.http.client.Realm.AuthScheme.BASIC)
    }

    private def logRequest(method: String, req: play.api.libs.ws.WS.WSRequestHolder)(implicit ec: scala.concurrent.ExecutionContext): play.api.libs.ws.WS.WSRequestHolder = {
      // auth should always be present, but just in case it isn't,
      // we'll supply a default
      val (apiToken, _, _) = req.auth.getOrElse(("", "",  com.ning.http.client.Realm.AuthScheme.BASIC))
      logger.info(s"curl -X $$method -u '[REDACTED]:' $${req.url}")
      req
    }

    private def processResponse(f: scala.concurrent.Future[play.api.libs.ws.Response])(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.Response] = {
      f.map { response =>
        logger.debug(response.body)
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
      processResponse(play.api.libs.ApidocWSHelper.patch(logRequest("PATCH", requestHolder(path)), data))
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
          } else if (response.multiple) {
            s"case r if r.status == ${response.code} => r.status -> r.json.as[List[$tpe]]"
          } else {
            s"case r if r.status == ${response.code} => r.status -> r.json.as[$tpe]"
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
