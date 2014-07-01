package client

import play.api.libs.json._
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.Play.current

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

  case class VersionsResource(client: Apidoc.Client) {

    def put(orgKey: String, serviceKey: String, version: String, file: java.io.File) = {
      client.wsUrl(s"/${orgKey}/${serviceKey}/${version}").withHeaders("Content-type" -> "application/json").put(file)
    }

  }

}
