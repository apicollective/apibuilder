package core.generator.ning

import core._
import core.generator._
import core.generator.ScalaUtil._
import Text._

case class NingVersion(
  name: String
)

object NingVersions {

  val V1_8_x = NingVersion(
    name = "1.8.x"
  )

}

/**
 * Uses play JSON libraries for json
 * serialization/deserialization. Otherwise only depends on ning async
 * http client.
 */
object NingClientGenerator {

  def generate(version: NingVersion, sd: ServiceDescription, userAgent: String): String = {
    val ssd = new ScalaServiceDescription(sd)
    generate(version, ssd, userAgent)
  }

  def generate(version: NingVersion, ssd: ScalaServiceDescription, userAgent: String): String = {
    NingClientGenerator(version, ssd, userAgent).generate()
  }

}

case class NingClientGenerator(version: NingVersion, ssd: ScalaServiceDescription, userAgent: String) {

  def generate(): String = {
    Seq(
      Play2Models(ssd),
      client()
    ).mkString("\n\n")
  }

  private[ning] def toJson(klass: String): String = {
    Seq(
      s"play.api.libs.json.Json.parse(response.getResponseBody(Encoding)).validate[${klass}] match {",
      s"""  case play.api.libs.json.JsSuccess(x, _) => x""",
      s"""  case play.api.libs.json.JsError(errors) => sys.error("Invalid json: " + errors.mkString(" "))""",
      s"}"
    ).mkString("\n")
  }

  private[ning] def errorTypeClass(response: ScalaResponse): String = {
    require(!response.isSuccess)

    // pass in status and UNPARSED body so that there is still a useful error
    // message even when the body is malformed and cannot be parsed
    Seq(
      s"""case class ${response.errorClassName}(response: com.ning.http.client.Response) extends Exception(response.status + ": " + response.body) {""",
      "",
      s"  import ${ssd.packageName}.models.json._",
      "",
      s"lazy val ${response.errorVariableName} = ${toJson(response.errorResponseType)}",
      ""
    ).mkString("\n")
  }

  private[ning] def errors(): Option[String] = {
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

    val headerString = (ssd.defaultHeaders ++ Seq(ScalaHeader("User-Agent", "UserAgent"))).map { h =>
      s""".addHeader("${h.name}", ${h.quotedValue}"""
    }.mkString("\n")

    val methodGenerator = ScalaClientMethodGenerator(ScalaClientMethodConfigs.Ning, ssd)

    s"""package ${ssd.packageName} {
  import com.ning.http.client.{AsyncCompletionHandler, AsyncHttpClient, Realm, Request, RequestBuilder, Response}

  object helpers {

${ScalaHelpers.dateTime}

  }

  class Client(apiUrl: String, apiToken: scala.Option[String] = None) {
    import ${ssd.packageName}.models.json._

    val asyncHttpClient = new AsyncHttpClient()
    private val UserAgent = "$userAgent"
    val Encoding = "UTF-8"

${methodGenerator.accessors().indent(4)}

${methodGenerator.objects().indent(4)}

    def _logRequest(request: Request) {
      println("_logRequest: " + request)
    }

    def _requestBuilder(method: String, path: String): RequestBuilder = {
      val builder = new RequestBuilder(method)
        .setUrl(apiUrl + path)
        .addHeader("User-Agent", UserAgent)

      apiToken.fold(builder) { token =>
        builder.setRealm(
          new Realm.RealmBuilder()
            .setPrincipal(token)
            .setScheme(Realm.AuthScheme.BASIC)
            .build()
        )
      }
    }

    def _executeRequest(
      method: String,
      path: String,
      q: Seq[(String, String)] = Seq.empty,
      body: Option[play.api.libs.json.JsObject] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.ning.http.client.Response] = {
      val request = _requestBuilder(method, path)

      q.foreach { pair =>
        request.addQueryParameter(pair._1, pair._2)
      }

      val requestWithParamsAndBody = body.fold(request) { b =>
        val serialized = play.api.libs.json.Json.stringify(b)
        request.setBody(serialized).addHeader("Content-type", "application/json")
      }

      val finalRequest = requestWithParamsAndBody.build()
      _logRequest(finalRequest)

      val result = scala.concurrent.Promise[com.ning.http.client.Response]()
      asyncHttpClient.executeRequest(finalRequest,
        new AsyncCompletionHandler[Unit]() {
          override def onCompleted(r: com.ning.http.client.Response) = result.success(r)
        }
      )
      result.future
    }

    def _parseJson[T](r: com.ning.http.client.Response, f: (play.api.libs.json.JsValue => play.api.libs.json.JsResult[T])): T = {
      f(play.api.libs.json.Json.parse(r.getResponseBody(Encoding))) match {
        case play.api.libs.json.JsSuccess(x, _) => x
        case play.api.libs.json.JsError(errors) => {
          throw new FailedRequest(r, Some("Invalid json: " + errors.mkString(" ")))
        }
      }
    }

  }

  ${methodGenerator.traits().indent(2)}

  case class FailedRequest(
    response: com.ning.http.client.Response,
    message: Option[String] = None
  ) extends Exception(message.getOrElse(response.getStatusCode() + ": " + response.getResponseBody(Encoding)))

}"""
  }

}
