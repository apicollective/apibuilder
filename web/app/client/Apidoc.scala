package client

import play.api.libs.json._
import play.api.libs.ws._
import scala.concurrent.Future

object Apidoc {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global
  private val BaseUrl = "http://localhost:9001"
  private val Token = "ZdRD61ODVPspeV8Wf18EmNuKNxUfjfROyJXtNJXj9GMMwrAxqi8I4aUtNAT6"

  lazy val organizations = OrganizationsResource(s"$BaseUrl/organizations")
  lazy val membershipRequests = MembershipRequestsResource(s"$BaseUrl/membership_requests")
  lazy val services = ServicesResource(BaseUrl)
  lazy val users = UsersResource(s"$BaseUrl/users")
  lazy val versions = VersionsResource(BaseUrl)

  def wsUrl(url: String) = {
    println("URL: " + url)
    WS.url(url).withHeaders("X-Auth" -> Token)
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
      wsUrl(url).withQueryString("guid" -> userGuid).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[User] }.headOption
      }
    }

    def findByEmail(email: String): Future[Option[User]] = {
      wsUrl(url).withQueryString("email" -> email).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[User] }.headOption
      }
    }

  }

  case class OrganizationsResource(url: String) {

    def findByKey(key: String): Future[Option[Organization]] = {
      wsUrl(url).withQueryString("key" -> key).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Organization] }.headOption
      }
    }

    def findByName(name: String): Future[Option[Organization]] = {
      wsUrl(url).withQueryString("name" -> name).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Organization] }.headOption
      }
    }

    def findAll(userGuid: String, limit: Int = 50, offset: Int = 0): Future[Seq[Organization]] = {
      wsUrl(url).withQueryString("user_guid" -> userGuid, "limit" -> limit.toString, "offset" -> offset.toString).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Organization] }
      }
    }

    def create(user: User, name: String): Future[Organization] = {
      val json = Json.obj(
        "user_guid" -> user.guid,
        "name" -> name
      )

      wsUrl(url).post(json).map { response =>
        response.json.as[Organization]
      }
    }

  }

  case class MembershipRequestsResource(url: String) {

    def findByGuid(guid: String): Future[Option[MembershipRequest]] = {
      wsUrl(url).withQueryString("guid" -> guid).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[MembershipRequest] }.headOption
      }
    }

    def findAll(userGuid: String, limit: Int = 50, offset: Int = 0): Future[Seq[MembershipRequest]] = {
      wsUrl(url).withQueryString("user_guid" -> userGuid, "limit" -> limit.toString, "offset" -> offset.toString).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[MembershipRequest] }
      }
    }

    def create(user: User, name: String): Future[MembershipRequest] = {
      val json = Json.obj(
        "user_guid" -> user.guid,
        "name" -> name
      )

      wsUrl(url).post(json).map { response =>
        response.json.as[MembershipRequest]
      }
    }

  }

  case class ServicesResource(url: String) {

    def findAllByOrganizationKey(orgKey: String): Future[Seq[Service]] = {
      wsUrl(url + s"/${orgKey}").get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Service] }
      }
    }

    def findByOrganizationKeyAndKey(orgKey: String, serviceKey: String): Future[Option[Service]] = {
      wsUrl(url + s"/${orgKey}").withQueryString("key" -> serviceKey).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Service] }.headOption
      }
    }

  }

  case class VersionsResource(url: String) {

    def findByOrganizationKeyAndServiceKeyAndVersion(orgKey: String, serviceKey: String, version: String): Future[Version] = {
      wsUrl(url + s"/${orgKey}/${serviceKey}/${version}").get().map { response =>
        response.json.as[Version]
      }
    }

    def findAllByOrganizationKeyAndServiceKey(orgKey: String, serviceKey: String, limit: Int = 50, offset: Int = 0): Future[Seq[Version]] = {
      wsUrl(url + s"/${orgKey}/${serviceKey}").withQueryString("limit" -> limit.toString, "offset" -> offset.toString).get().map { response =>
        response.json.as[JsArray].value.map { v => v.as[Version] }
      }
    }

  }

}
