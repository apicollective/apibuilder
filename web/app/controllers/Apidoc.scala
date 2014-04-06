package controllers

import core._
import play.api.libs.json._
import play.api.libs.ws._
import scala.concurrent.Future
import java.util.UUID

object Apidoc {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  private lazy val baseUrl = "http://localhost:9001"

  lazy val organizations = OrganizationsResource(s"$baseUrl/organizations")
  lazy val users = UsersResource(s"$baseUrl/users")

  case class UsersResource(url: String) {

    def findByGuid(userGuid: String): Future[Option[User]] = {
      val token = "12345"
      //val query = UserQuery(guid = Some(userGuid), limit = 1)
      println("URL: " + url)
      WS.url(url).withHeaders("X-Auth" -> token).withQueryString("guid" -> userGuid).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[User] }.headOption
      }
    }

    /*
    def findAll(query: UserQuery): Future[Seq[User]] = {
      println("URL: " + url)
      * WS.url(url).withQueryString(query.params).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[User] }
      }
    }
    */
  }

  case class OrganizationsResource(url: String) {

    def findByKey(key: String): Future[Option[Organization]] = {
      println("URL: " + url)
      WS.url(url).withQueryString(key -> key).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Organization] }.headOption
      }
    }

    def findAll(query: OrganizationQuery): Future[Seq[Organization]] = {
      // TODO: query parameters
      println("URL: " + url)
      WS.url(url).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Organization] }
      }
    }
  }

}
