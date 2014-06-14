package client

import play.api.libs.json._
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.Play.current

object ApidocClient {

  val apiUrl = current.configuration.getString("apidoc.url").getOrElse {
    sys.error("apidoc.url is required")
  }

  def token(guid: String): String = Tokens.get(guid).getOrElse {
    sys.error(s"No token for user guid[$guid]")
  }

  private val Tokens = Map(
    "f3973f60-be9f-11e3-b1b6-0800200c9a66" -> "ZdRD61ODVPspeV8Wf18EmNuKNxUfjfROyJXtNJXj9GMMwrAxqi8I4aUtNAT6",
    "1c59f283-353d-4d91-bff2-0a0e2956fefe" -> "1c59f283-353d-4d91-bff2-0a0e2956fefe",
    "c6a7ca2f-4f67-4079-8314-f5812d44946f" -> "c6a7ca2f-4f67-4079-8314-f5812d44946f"
  )

  def instance(userGuid: String): Apidoc.Client = {
    Apidoc.Client(baseUrl = apiUrl, token = token(userGuid))
  }

}



object Apidoc {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  case class Client(baseUrl: String, token: String) {

    private val Password = ""

    def wsUrl(url: String) = {
      println("curl -u \"" + token + ":\" " + baseUrl + url)
      WS.url(baseUrl + url).withAuth(token, Password, WSAuthScheme.BASIC)
    }

    lazy val versions = VersionsResource(this)

  }

  case class Version(guid: String, version: String, json: Option[String])
  object Version {
    implicit val versionReads = Json.reads[Version]
  }

  case class VersionsResource(client: Apidoc.Client) {

    def findByOrganizationKeyAndServiceKeyAndVersion(orgKey: String, serviceKey: String, version: String): Future[Option[Version]] = {
      client.wsUrl(s"/${orgKey}/${serviceKey}/${version}").get().map { response =>
        Some(response.json.as[Version])
      }
    }

    def findAllByOrganizationKeyAndServiceKey(orgKey: String, serviceKey: String, limit: Int = 50, offset: Int = 0): Future[Seq[Version]] = {
      client.wsUrl(s"/${orgKey}/${serviceKey}").withQueryString("limit" -> limit.toString, "offset" -> offset.toString).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Version] }
      }
    }

    def put(orgKey: String, serviceKey: String, version: String, file: java.io.File) = {
      client.wsUrl(s"/${orgKey}/${serviceKey}/${version}").withHeaders("Content-type" -> "application/json").put(file)
    }

  }

}
