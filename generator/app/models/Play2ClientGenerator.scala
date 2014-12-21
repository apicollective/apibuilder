package models

import com.gilt.apidocspec.models.Service
import core._
import lib.Text._
import generator.{ScalaClientMethodGenerator, ScalaService, CodeGenerator, ScalaClientMethodConfigs, ScalaClientMethodConfig}

case class PlayFrameworkVersion(
  name: String,
  config: ScalaClientMethodConfig,
  requestHolderClass: String,
  authSchemeClass: String,
  supportsHttpPatch: Boolean
)

object PlayFrameworkVersions {

  val V2_2_x = PlayFrameworkVersion(
    name = "2.2.x",
    config = ScalaClientMethodConfigs.Play22,
    requestHolderClass = "play.api.libs.ws.WS.WSRequestHolder",
    authSchemeClass = "com.ning.http.client.Realm.AuthScheme",
    supportsHttpPatch = false
  )

  val V2_3_x = PlayFrameworkVersion(
    name = "2.3.x",
    config = ScalaClientMethodConfigs.Play23,
    requestHolderClass = "play.api.libs.ws.WSRequestHolder",
    authSchemeClass = "play.api.libs.ws.WSAuthScheme",
    supportsHttpPatch = true
  )
}

object Play22ClientGenerator extends CodeGenerator {
  override def generate(sd: Service): String = {
    Play2ClientGenerator.generate(PlayFrameworkVersions.V2_2_x, sd)
  }
}

object Play23ClientGenerator extends CodeGenerator {
  override def generate(sd: Service): String = {
    Play2ClientGenerator.generate(PlayFrameworkVersions.V2_3_x, sd)
  }
}

object Play2ClientGenerator {

  def generate(version: PlayFrameworkVersion, sd: Service): String = {
    val ssd = new ScalaService(sd)
    generate(version, ssd)
  }

  def generate(version: PlayFrameworkVersion, ssd: ScalaService): String = {
    Play2ClientGenerator(version, ssd).generate()
  }

}

case class Play2ClientGenerator(version: PlayFrameworkVersion, ssd: ScalaService) {

  def generate(): String = {
    ApidocHeaders(ssd.serviceDescription.userAgent).toJavaString + "\n" +
    Seq(
      Play2Models(ssd, addHeader = false),
      client()
    ).mkString("\n\n")
  }

  private def client(): String = {

    val methodGenerator = ScalaClientMethodGenerator(version.config, ssd)

    val bindables = Play2Bindables.build(ssd).indent(2)

    val patchMethod = version.supportsHttpPatch match {
      case true => """_logRequest("PATCH", _requestHolder(path).withQueryString(queryParameters:_*)).patch(body.getOrElse(play.api.libs.json.Json.obj()))"""
      case false => s"""sys.error("PATCH method is not supported in Play Framework Version ${version.name}")"""
    }

    val headerString = ".withHeaders(" +
      (ssd.defaultHeaders.map { h =>
        s""""${h.name}" -> ${h.quotedValue}"""
      } ++  Seq(""""User-Agent" -> UserAgent""")).mkString(", ") + ")"

    s"""package ${ssd.packageName} {

  class Client(apiUrl: String, apiToken: scala.Option[String] = None) {
    import ${ssd.modelPackageName}.json._

    private val UserAgent = "${ssd.serviceDescription.userAgent.getOrElse("unknown")}"
    private val logger = play.api.Logger("${ssd.packageName}.client")

    logger.info(s"Initializing ${ssd.packageName}.client for url $$apiUrl")

${methodGenerator.accessors().indent(4)}

${methodGenerator.objects().indent(4)}

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

    def _executeRequest(
      method: String,
      path: String,
      queryParameters: Seq[(String, String)] = Seq.empty,
      body: Option[play.api.libs.json.JsValue] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[${version.config.responseClass}] = {
      method.toUpperCase match {
        case "GET" => {
          _logRequest("GET", _requestHolder(path).withQueryString(queryParameters:_*)).get()
        }
        case "POST" => {
          _logRequest("POST", _requestHolder(path).withQueryString(queryParameters:_*)).post(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PUT" => {
          _logRequest("PUT", _requestHolder(path).withQueryString(queryParameters:_*)).put(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PATCH" => {
          $patchMethod
        }
        case "DELETE" => {
          _logRequest("DELETE", _requestHolder(path).withQueryString(queryParameters:_*)).delete()
        }
         case "HEAD" => {
          _logRequest("HEAD", _requestHolder(path).withQueryString(queryParameters:_*)).head()
        }
         case "OPTIONS" => {
          _logRequest("OPTIONS", _requestHolder(path).withQueryString(queryParameters:_*)).options()
        }
        case _ => {
          _logRequest(method, _requestHolder(path).withQueryString(queryParameters:_*))
          sys.error("Unsupported method[%s]".format(method))
        }
      }
    }

  }

${methodGenerator.traitsAndErrors().indent(2)}

$bindables

}"""
  }

}
