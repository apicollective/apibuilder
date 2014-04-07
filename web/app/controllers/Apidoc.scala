package controllers

import core._
import play.api.libs.json._
import play.api.libs.ws._
import scala.concurrent.Future

object Apidoc {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global
  private val BaseUrl = "http://localhost:9001"
  private val Token = "12345"

  lazy val organizations = OrganizationsResource(s"$BaseUrl/organizations")
  lazy val users = UsersResource(s"$BaseUrl/users")

  def wsUrl(url: String) = {
    println("URL: " + url)
    WS.url(url).withHeaders("X-Auth" -> Token)
  }

  case class UsersResource(url: String) {

    def update(user: User): Future[User] = {
      val json = Json.obj(
        "email" -> user.email,
        "name" -> user.name,
        "image_url" -> user.imageUrl
      )

      wsUrl(url + s"/${user.guid}").put(json).map { response =>
        response.json.as[JsArray].value.map { v => v.as[User] }.head
      }
    }

    def create(email: String, name: Option[String], imageUrl: Option[String]): Future[User] = {
      val json = Json.obj(
        "email" -> email,
        "name" -> name,
        "image_url" -> imageUrl
      )

      wsUrl(url).post(json).map { response =>
        response.json.as[JsArray].value.map { v => v.as[User] }.head
      }
    }

    def findByGuid(userGuid: String): Future[Option[User]] = {
      //val query = UserQuery(guid = Some(userGuid), limit = 1)
      wsUrl(url).withQueryString("guid" -> userGuid).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[User] }.headOption
      }
    }

    def findByEmail(email: String): Future[Option[User]] = {
      //val query = UserQuery(email = Some(email), limit = 1)
      wsUrl(url).withQueryString("email" -> email).get().map { response =>
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
      wsUrl(url).withQueryString(key -> key).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Organization] }.headOption
      }
    }

    def findByName(name: String): Future[Option[Organization]] = {
      wsUrl(url).withQueryString(name -> name).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Organization] }.headOption
      }
    }

    def findAll(query: OrganizationQuery): Future[Seq[Organization]] = {
      // TODO: query parameters
      wsUrl(url).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Organization] }
      }
    }

    def create(user: User, name: String): Future[Organization] = {
      val json = Json.obj(
        "user_guid" -> user.guid,
        "name" -> name
      )

      wsUrl(url).post(json).map { response =>
        response.json.as[JsArray].value.map { v => v.as[Organization] }.head
      }
    }

  }

}
