package client

import play.api.libs.json._
import play.api.libs.ws._
import scala.concurrent.Future
import com.ning.http.client.Realm.AuthScheme

object ApidocClient {

  def instance(userGuid: String): Apidoc.Client = {
    Apidoc.Client(baseUrl = "http://localhost:9001",
                  token = "ZdRD61ODVPspeV8Wf18EmNuKNxUfjfROyJXtNJXj9GMMwrAxqi8I4aUtNAT6")
  }

}



object Apidoc {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  case class Client(baseUrl: String, token: String) {

    private val Password = ""

    def wsUrl(url: String) = {
      println("URL: " + baseUrl + url)
      WS.url(baseUrl + url).withAuth(token, Password, AuthScheme.BASIC)
    }

    lazy val organizations = OrganizationsResource(this)
    lazy val membershipRequests = MembershipRequestsResource(this)
    lazy val services = ServicesResource(this)
    lazy val users = UsersResource(this)
    lazy val versions = VersionsResource(this)

  }

  case class Organization(guid: String, name: String, key: String)
  object Organization {
    implicit val organizationReads = Json.reads[Organization]
  }

  case class MembershipRequest(guid: String, user_guid: String, organization_guid: String, role: String)
  object MembershipRequest {
    implicit val membershipRequestReads = Json.reads[MembershipRequest]
  }

  case class User(guid: String, email: String, name: Option[String], imageUrl: Option[String])
  object User {
    implicit val userReads = Json.reads[User]
  }

  case class Version(guid: String, version: String, json: Option[String])
  object Version {
    implicit val versionReads = Json.reads[Version]
  }

  case class Service(guid: String, name: String, key: String, description: Option[String])
  object Service {
    implicit val serviceReads = Json.reads[Service]
  }

  case class UsersResource(client: Apidoc.Client) {

    def update(user: User): Future[User] = {
      val json = Json.obj(
        "email" -> user.email,
        "name" -> user.name,
        "image_url" -> user.imageUrl
      )

      client.wsUrl(s"/users/${user.guid}").put(json).map { response =>
        response.json.as[JsArray].value.map { v => v.as[User] }.head
      }
    }

    def create(email: String, name: Option[String], imageUrl: Option[String]): Future[User] = {
      val json = Json.obj(
        "email" -> email,
        "name" -> name,
        "image_url" -> imageUrl
      )

      client.wsUrl("/users").post(json).map { response =>
        response.json.as[JsArray].value.map { v => v.as[User] }.head
      }
    }

    def findByGuid(userGuid: String): Future[Option[User]] = {
      client.wsUrl("/users").withQueryString("guid" -> userGuid).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[User] }.headOption
      }
    }

    def findByEmail(email: String): Future[Option[User]] = {
      client.wsUrl("/users").withQueryString("email" -> email).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[User] }.headOption
      }
    }

  }

  case class OrganizationsResource(client: Client) {

    def findByKey(key: String): Future[Option[Organization]] = {
      client.wsUrl("/organizations").withQueryString("key" -> key).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Organization] }.headOption
      }
    }

    def findByName(name: String): Future[Option[Organization]] = {
      client.wsUrl("/organizations").withQueryString("name" -> name).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Organization] }.headOption
      }
    }

    def findAll(userGuid: String, limit: Int = 50, offset: Int = 0): Future[Seq[Organization]] = {
      client.wsUrl("/organizations").withQueryString("user_guid" -> userGuid, "limit" -> limit.toString, "offset" -> offset.toString).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Organization] }
      }
    }

    def create(user: User, name: String): Future[Organization] = {
      val json = Json.obj(
        "user_guid" -> user.guid,
        "name" -> name
      )

      client.wsUrl("/organizations").post(json).map { response =>
        response.json.as[Organization]
      }
    }

  }

  case class MembershipRequestsResource(client: Apidoc.Client) {

    def findByGuid(guid: String): Future[Option[MembershipRequest]] = {
      client.wsUrl("/membership_requests").withQueryString("guid" -> guid).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[MembershipRequest] }.headOption
      }
    }

    def findAll(userGuid: String, limit: Int = 50, offset: Int = 0): Future[Seq[MembershipRequest]] = {
      client.wsUrl("/membership_requests").withQueryString("user_guid" -> userGuid, "limit" -> limit.toString, "offset" -> offset.toString).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[MembershipRequest] }
      }
    }

    def create(user: User, name: String): Future[MembershipRequest] = {
      val json = Json.obj(
        "user_guid" -> user.guid,
        "name" -> name
      )

      client.wsUrl("/membership_requests").post(json).map { response =>
        response.json.as[MembershipRequest]
      }
    }

  }

  case class ServicesResource(client: Apidoc.Client) {

    def findAllByOrganizationKey(orgKey: String): Future[Seq[Service]] = {
      client.wsUrl(s"/${orgKey}").get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Service] }
      }
    }

    def findByOrganizationKeyAndKey(orgKey: String, serviceKey: String): Future[Option[Service]] = {
      client.wsUrl(s"/${orgKey}").withQueryString("key" -> serviceKey).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Service] }.headOption
      }
    }

  }

  case class VersionsResource(client: Apidoc.Client) {

    def findByOrganizationKeyAndServiceKeyAndVersion(orgKey: String, serviceKey: String, version: String): Future[Option[Version]] = {
      client.wsUrl(s"/versions/${orgKey}/${serviceKey}/${version}").get().map { response =>
        // TODO: If a 404, return none
        try {
          Some(response.json.as[Version])
        } catch {
          case _: Throwable => None
        }
      }
    }

    def findAllByOrganizationKeyAndServiceKey(orgKey: String, serviceKey: String, limit: Int = 50, offset: Int = 0): Future[Seq[Version]] = {
      client.wsUrl(s"/versions/${orgKey}/${serviceKey}").withQueryString("limit" -> limit.toString, "offset" -> offset.toString).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Version] }
      }
    }

    def put(orgKey: String, serviceKey: String, version: String, file: java.io.File) = {
      client.wsUrl(s"/versions/${orgKey}/${serviceKey}/${version}").withHeaders("Content-type" -> "application/json").put(file)
    }

  }

}
