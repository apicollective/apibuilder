package core.generator

import core._
import Text._
import ScalaUtil._

case class PlayFrameworkVersion(
  name: String,
  responseClass: String,
  requestHolderClass: String,
  authSchemeClass: String,
  supportsHttpPatch: Boolean
)

object PlayFrameworkVersions {

  val V2_2_x = PlayFrameworkVersion(
    name = "2.2.x",
    responseClass = "play.api.libs.ws.Response",
    requestHolderClass = "play.api.libs.ws.WS.WSRequestHolder",
    authSchemeClass = "com.ning.http.client.Realm.AuthScheme",
    supportsHttpPatch = false
  )

  val V2_3_x = PlayFrameworkVersion(
    name = "2.3.x",
    responseClass = "play.api.libs.ws.WSResponse",
    requestHolderClass = "play.api.libs.ws.WSRequestHolder",
    authSchemeClass = "play.api.libs.ws.WSAuthScheme",
    supportsHttpPatch = true
  )
}

object Play2ClientGenerator {

  def generate(version: PlayFrameworkVersion, sd: ServiceDescription, userAgent: String): String = {
    val ssd = new ScalaServiceDescription(sd)
    generate(version, ssd, userAgent)
  }

  def generate(version: PlayFrameworkVersion, ssd: ScalaServiceDescription, userAgent: String): String = {
    Play2ClientGenerator(version, ssd, userAgent).generate()
  }

}

case class Play2ClientGenerator(version: PlayFrameworkVersion, ssd: ScalaServiceDescription, userAgent: String) {

  def generate(): String = {
    Seq(
      Play2Models(ssd),
      client()
    ).mkString("\n\n")
  }

  private[generator] def errorTypeClass(response: ScalaResponse): String = {
    require(!response.isSuccess)

    // pass in status and UNPARSED body so that there is still a useful error
    // message even when the body is malformed and cannot be parsed
    Seq(
      s"""case class ${response.errorClassName}(response: ${version.responseClass}) extends Exception(response.status + ": " + response.body) {""",
      "",
      s"  lazy val ${response.errorVariableName} = response.json.as[${response.errorResponseType}]",
      "",
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
          s"  import ${ssd.packageName}.models.json._",
          "",
          errorTypes.map { t => errorTypeClass(t) }.distinct.sorted.mkString("\n\n").indent(2),
          "}"
        ).mkString("\n").indent(2)
      )
    }
  }

  private def client(): String = {
    val errorsString = errors() match {
      case None => ""
      case Some(s: String) => s"\n\n$s\n"
    }

    val accessors = ssd.resources.map(_.model.plural).sorted.map { plural =>
      val methodName = Text.snakeToCamelCase(Text.camelCaseToUnderscore(plural).toLowerCase)
      s"def ${methodName}: ${plural} = ${plural}"
    }.mkString("\n\n")

    val patchMethod = version.supportsHttpPatch match {
      case true => """_logRequest("PATCH", _requestHolder(path).withQueryString(q:_*)).patch(data)"""
      case false => s"""sys.error("PATCH method is not supported in Play Framework Version ${version.name}")"""
    }

    val headerString = ".withHeaders(" +
    (ssd.defaultHeaders ++ Seq(Header("User-Agent", "UserAgent"))).map { h =>
      s""""${h.name}" -> ${h.value}"""
    }.mkString(", ") + ")"

    s"""package ${ssd.packageName} {
  object helpers {
    import org.joda.time.DateTime
    import org.joda.time.format.ISODateTimeFormat
    import play.api.mvc.QueryStringBindable

    import scala.util.{ Failure, Success, Try }

    private[helpers] val dateTimeISOParser = ISODateTimeFormat.dateTimeParser()
    private[helpers] val dateTimeISOFormatter = ISODateTimeFormat.dateTime()

    private[helpers] def parseDateTimeISO(s: String): Either[String, DateTime] = {
      Try(dateTimeISOParser.parseDateTime(s)) match {
        case Success(dt) => Right(dt)
        case Failure(f) => Left("Could not parse DateTime: " + f.getMessage)
      }
    }

    implicit object DateTimeISOQueryStringBinder extends QueryStringBindable[DateTime] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, DateTime]] = {
        for {
          values <- params.get(key)
          s <- values.headOption
        } yield parseDateTimeISO(s)
      }

      override def unbind(key: String, time: DateTime): String = key + "=" + dateTimeISOFormatter.print(time)
    }
  }

  class Client(apiUrl: String, apiToken: scala.Option[String] = None) {
    import ${ssd.packageName}.models._
    import ${ssd.packageName}.models.json._

    private val UserAgent = "$userAgent"
    private val logger = play.api.Logger("${ssd.packageName}.client")

    logger.info(s"Initializing ${ssd.packageName}.client for url $$apiUrl")

${accessors.indent(4)}

${modelClientObjects().indent(4)}

    def _requestHolder(path: String): ${version.requestHolderClass} = {
      import play.api.Play.current

      val holder = play.api.libs.ws.WS.url(apiUrl + path)$headerString
      apiToken.fold(holder) { token =>
        holder.withAuth(token, "", ${version.authSchemeClass}.BASIC)
      }
    }

    def _logRequest(method: String, req: ${version.requestHolderClass})(implicit ec: scala.concurrent.ExecutionContext): ${version.requestHolderClass} = {
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

    def POST(
      path: String,
      data: play.api.libs.json.JsValue = play.api.libs.json.Json.obj(),
      q: Seq[(String, String)] = Seq.empty
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[${version.responseClass}] = {
      _logRequest("POST", _requestHolder(path).withQueryString(q:_*)).post(data)
    }

    def GET(
      path: String,
      q: Seq[(String, String)] = Seq.empty
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[${version.responseClass}] = {
      _logRequest("GET", _requestHolder(path).withQueryString(q:_*)).get()
    }

    def PUT(
      path: String,
      data: play.api.libs.json.JsValue = play.api.libs.json.Json.obj(),
      q: Seq[(String, String)] = Seq.empty
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[${version.responseClass}] = {
      _logRequest("PUT", _requestHolder(path).withQueryString(q:_*)).put(data)
    }

    def PATCH(
      path: String,
      data: play.api.libs.json.JsValue = play.api.libs.json.Json.obj(),
      q: Seq[(String, String)] = Seq.empty
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[${version.responseClass}] = {
      $patchMethod
    }

    def DELETE(
      path: String,
      q: Seq[(String, String)] = Seq.empty
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[${version.responseClass}] = {
      _logRequest("DELETE", _requestHolder(path).withQueryString(q:_*)).delete()
    }

  }

${modelClientTraits().indent(2)}

  case class FailedRequest(response: ${version.responseClass}) extends Exception(response.status + ": " + response.body)$errorsString

}"""
  }

  private def modelClientTraits(): String = {
    ssd.resources.groupBy(_.model.plural).toSeq.sortBy(_._1).map { case (plural, resources) =>
      s"trait $plural {\n" +
      clientMethods(resources).map(_.interface).mkString("\n\n").indent(2) +
      "\n}"
    }.mkString("\n\n")
  }

  private def modelClientObjects(): String = {
    ssd.resources.groupBy(_.model.plural).toSeq.sortBy(_._1).map { case (plural, resources) =>
      s"object $plural extends $plural {\n" +
      clientMethods(resources).map(_.code).mkString("\n\n").indent(2) +
      "\n}"
    }.mkString("\n\n")
  }

  private def clientMethods(resources: Seq[ScalaResource]): Seq[ClientMethod] = {
    resources.flatMap(_.operations).map { op =>
      val path = Play2Util.pathParams(op)

      val methodCall = if (Util.isJsonDocumentMethod(op.method)) {
        val payload = Play2Util.formBody(op)
        val query = Play2Util.queryParams(op)

        if (payload.isEmpty && query.isEmpty) {
          s"${op.method}($path)"

        } else if (!payload.isEmpty && !query.isEmpty) {
          s"${payload.get}\n\n${query.get}\n\n${op.method}($path, payload, query)"

        } else if (payload.isEmpty) {
          s"${query.get}\n\n${op.method}(path = $path, q = query)"

        } else {
          s"${payload.get}\n\n${op.method}($path, payload)"

        }

      } else {
        Play2Util.queryParams(op) match {
          case None => s"${op.method}($path)"
          case Some(query) => s"${query}\n\n${op.method}($path, query)"
        }
      }

      val hasOptionResult = op.responses.filter(_.isSuccess).find(_.isOption) match {
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
      } + hasOptionResult + "\ncase r => throw new FailedRequest(r)\n"

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
      s"""${commentString}def $name(${argList.getOrElse("")})(implicit ec: scala.concurrent.ExecutionContext): $returnType"""
    }

    val code: String = {
      s"""override def $name(${argList.getOrElse("")})(implicit ec: scala.concurrent.ExecutionContext): $returnType = {
${methodCall.indent}.map {
${response.indent(4)}
  }
}"""
    }
  }

}
