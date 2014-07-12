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

  private[generator] def errorTypeClass(response: ScalaResponse): String = {
    require(!response.isSuccess)

    val underscore = Text.camelCaseToUnderscore(response.scalaType)
    val label = if (response.isMultiple) {
      Text.snakeToCamelCase(Text.pluralize(underscore.toLowerCase))
    } else {
      Text.snakeToCamelCase(underscore)
    }
    val labelInitCap = Text.initCap(label)

    val result = Seq(
      s"case class ${labelInitCap}Response(response: play.api.libs.ws.Response) extends Exception {",
      "",
      s"  lazy val $label: ${response.resultType} = response.json.as[${response.resultType}]",
      "",
      "}"
    ).mkString("\n")

    println(result)
    result
  }

  private def errors(ssd: ScalaServiceDescription): Option[String] = {
    val errorTypes = ssd.resources.flatMap(_.operations).flatMap(_.responses).filter(!_.isSuccess)

    if (errorTypes.isEmpty) {
      None
    } else {
      println("errorTypes: " + errorTypeClass(errorTypes.head))

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
      case Some(s: String) => {
        println("S: " + s)
        s"\n\n$s\n"
      }
    }

s"""package ${ssd.packageName} {

  case class FailedResponse(response: play.api.libs.ws.Response) extends Exception$errorsString

  class Client(apiUrl: String, apiToken: scala.Option[String] = None) {
    import ${ssd.packageName}.models._
    import ${ssd.packageName}.models.json._

    private val logger = play.api.Logger("${ssd.packageName}.client")

    logger.info(s"Initializing ${ssd.packageName}.client for url $$apiUrl")

    def requestHolder(path: String): play.api.libs.ws.WSRequestHolder = {
      import play.api.Play.current

      val holder = play.api.libs.ws.WS.url(apiUrl + path)
      apiToken.fold(holder) { token =>
        holder.withAuth(token, "", play.api.libs.ws.WSAuthScheme.BASIC)
      }
    }

    def logRequest(method: String, req: play.api.libs.ws.WSRequestHolder)(implicit ec: scala.concurrent.ExecutionContext): play.api.libs.ws.WSRequestHolder = {
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
              if (response.scalaType == "Unit") {
                s"case r if r.status == ${response.code} => Some(Unit)"
              } else {
                s"case r if r.status == ${response.code} => Some(r.json.as[${response.scalaType}])"
              }

            } else if (response.isMultiple) {
              s"case r if r.status == ${response.code} => r.json.as[scala.collection.Seq[${response.scalaType}]]"

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
