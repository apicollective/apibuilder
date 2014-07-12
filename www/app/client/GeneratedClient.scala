package apidoc.models {
  case class Code(
    target: String,
    source: String
  )
  case class Error(
    code: String,
    message: String
  )
  case class Healthcheck(
    status: String
  )
  case class Membership(
    guid: java.util.UUID,
    user: User,
    organization: Organization,
    role: String
  )
  case class MembershipRequest(
    guid: java.util.UUID,
    user: User,
    organization: Organization,
    role: String
  )
  case class Organization(
    guid: java.util.UUID,
    key: String,
    name: String
  )
  case class Service(
    guid: java.util.UUID,
    name: String,
    key: String,
    description: scala.Option[String] = None
  )
  case class User(
    guid: java.util.UUID,
    email: String,
    name: scala.Option[String] = None
  )
  case class Version(
    guid: java.util.UUID,
    version: String,
    json: String
  )
}

package apidoc.models {
  package object json {
    import play.api.libs.json._
    import play.api.libs.functional.syntax._

    implicit val jsonReadsUUID = __.read[String].map(java.util.UUID.fromString)

    implicit val jsonWritesUUID = new Writes[java.util.UUID] {
      def writes(x: java.util.UUID) = JsString(x.toString)
    }

    implicit val jsonReadsJodaDateTime = __.read[String].map { str =>
      import org.joda.time.format.ISODateTimeFormat.dateTimeParser
      dateTimeParser.parseDateTime(str)
    }

    implicit val jsonWritesJodaDateTime = new Writes[org.joda.time.DateTime] {
      def writes(x: org.joda.time.DateTime) = {
        import org.joda.time.format.ISODateTimeFormat.dateTime
        val str = dateTime.print(x)
        JsString(str)
      }
    }

    

    implicit def readsCode: play.api.libs.json.Reads[Code] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "target").read[String] and
         (__ \ "source").read[String])(Code.apply _)
      }
    
    implicit def writesCode: play.api.libs.json.Writes[Code] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "target").write[String] and
         (__ \ "source").write[String])(unlift(Code.unapply))
      }
    
    implicit def readsError: play.api.libs.json.Reads[Error] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "code").read[String] and
         (__ \ "message").read[String])(Error.apply _)
      }
    
    implicit def writesError: play.api.libs.json.Writes[Error] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "code").write[String] and
         (__ \ "message").write[String])(unlift(Error.unapply))
      }
    
    implicit def readsHealthcheck: play.api.libs.json.Reads[Healthcheck] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        (__ \ "status").read[String].map { x =>
          new Healthcheck(status = x)
        }
      }
    
    implicit def writesHealthcheck: play.api.libs.json.Writes[Healthcheck] =
      new play.api.libs.json.Writes[Healthcheck] {
        def writes(x: Healthcheck) = play.api.libs.json.Json.obj(
          "status" -> play.api.libs.json.Json.toJson(x.status)
        )
      }
    
    implicit def readsMembership: play.api.libs.json.Reads[Membership] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "user").read[User] and
         (__ \ "organization").read[Organization] and
         (__ \ "role").read[String])(Membership.apply _)
      }
    
    implicit def writesMembership: play.api.libs.json.Writes[Membership] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "user").write[User] and
         (__ \ "organization").write[Organization] and
         (__ \ "role").write[String])(unlift(Membership.unapply))
      }
    
    implicit def readsMembershipRequest: play.api.libs.json.Reads[MembershipRequest] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "user").read[User] and
         (__ \ "organization").read[Organization] and
         (__ \ "role").read[String])(MembershipRequest.apply _)
      }
    
    implicit def writesMembershipRequest: play.api.libs.json.Writes[MembershipRequest] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "user").write[User] and
         (__ \ "organization").write[Organization] and
         (__ \ "role").write[String])(unlift(MembershipRequest.unapply))
      }
    
    implicit def readsOrganization: play.api.libs.json.Reads[Organization] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "key").read[String] and
         (__ \ "name").read[String])(Organization.apply _)
      }
    
    implicit def writesOrganization: play.api.libs.json.Writes[Organization] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "key").write[String] and
         (__ \ "name").write[String])(unlift(Organization.unapply))
      }
    
    implicit def readsService: play.api.libs.json.Reads[Service] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "name").read[String] and
         (__ \ "key").read[String] and
         (__ \ "description").readNullable[String])(Service.apply _)
      }
    
    implicit def writesService: play.api.libs.json.Writes[Service] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "name").write[String] and
         (__ \ "key").write[String] and
         (__ \ "description").write[scala.Option[String]])(unlift(Service.unapply))
      }
    
    implicit def readsUser: play.api.libs.json.Reads[User] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "email").read[String] and
         (__ \ "name").readNullable[String])(User.apply _)
      }
    
    implicit def writesUser: play.api.libs.json.Writes[User] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "email").write[String] and
         (__ \ "name").write[scala.Option[String]])(unlift(User.unapply))
      }
    
    implicit def readsVersion: play.api.libs.json.Reads[Version] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "version").read[String] and
         (__ \ "json").read[String])(Version.apply _)
      }
    
    implicit def writesVersion: play.api.libs.json.Writes[Version] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "version").write[String] and
         (__ \ "json").write[String])(unlift(Version.unapply))
      }
  }
}

package apidoc {

  case class FailedResponse(response: play.api.libs.ws.Response) extends Exception

  package error {
  
    import apidoc.models.json._
  
    case class ErrorsResponse(response: play.api.libs.ws.Response) extends Exception {
    
      lazy val errors = response.json.as[scala.collection.Seq[apidoc.models.Error]]
    
    }
  }


  class Client(apiUrl: String, apiToken: scala.Option[String] = None) {
    import apidoc.models._
    import apidoc.models.json._

    private val logger = play.api.Logger("apidoc.client")

    logger.info(s"Initializing apidoc.client for url $apiUrl")

    def code = Code
    
    def healthchecks = Healthchecks
    
    def membershipRequests = MembershipRequests
    
    def memberships = Memberships
    
    def organizations = Organizations
    
    def services = Services
    
    def users = Users
    
    def versions = Versions

    def _requestHolder(path: String): play.api.libs.ws.WSRequestHolder = {
      import play.api.Play.current

      val holder = play.api.libs.ws.WS.url(apiUrl + path)
      apiToken.fold(holder) { token =>
        holder.withAuth(token, "", play.api.libs.ws.WSAuthScheme.BASIC)
      }
    }

    def _logRequest(method: String, req: play.api.libs.ws.WSRequestHolder)(implicit ec: scala.concurrent.ExecutionContext): play.api.libs.ws.WSRequestHolder = {
      val queryComponents = for {
        (name, values) <- req.queryString
        value <- values
      } yield name -> value
      val url = s"${req.url}${queryComponents.mkString("?", "&", "")}"
      apiToken.fold(logger.info(s"curl -X $method $url")) { _ =>
        logger.info(s"curl -X $method -u '[REDACTED]:' $url")
      }
      req
    }

    private def POST(path: String, data: play.api.libs.json.JsValue = play.api.libs.json.Json.obj())(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("POST", _requestHolder(path)).post(data)
    }

    private def GET(path: String, q: Seq[(String, String)] = Seq.empty)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("GET", _requestHolder(path).withQueryString(q:_*)).get()
    }

    private def PUT(path: String, data: play.api.libs.json.JsValue = play.api.libs.json.Json.obj())(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("PUT", _requestHolder(path)).put(data)
    }

    private def PATCH(path: String, data: play.api.libs.json.JsValue = play.api.libs.json.Json.obj())(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("PATCH", _requestHolder(path)).patch(data)
    }

    private def DELETE(path: String)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("DELETE", _requestHolder(path)).delete()
    }

    trait Code {
      /**
       * Generate code for a specific version of a service.
       */
      def getByOrgKeyAndServiceKeyAndVersionAndTargetName(
        orgKey: String,
        serviceKey: String,
        version: String,
        targetName: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Option[apidoc.models.Code]]
    }
  
    object Code extends Code {
      override def getByOrgKeyAndServiceKeyAndVersionAndTargetName(
        orgKey: String,
        serviceKey: String,
        version: String,
        targetName: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Option[apidoc.models.Code]] = {
        GET(s"/${java.net.URLEncoder.encode(orgKey, "UTF-8")}/${java.net.URLEncoder.encode(serviceKey, "UTF-8")}/${java.net.URLEncoder.encode(version, "UTF-8")}/${java.net.URLEncoder.encode(targetName, "UTF-8")}").map {
          case r if r.status == 200 => Some(r.json.as[apidoc.models.Code])
          case r if r.status == 409 => throw new apidoc.error.ErrorsResponse(r)
          case r if r.status == 404 => None
          case r => throw new FailedResponse(r)
        }
      }
    }
  
    trait Healthchecks {
      def get()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[apidoc.models.Healthcheck]]
    }
  
    object Healthchecks extends Healthchecks {
      override def get()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[apidoc.models.Healthcheck]] = {
        GET(s"/_internal_/healthcheck").map {
          case r if r.status == 200 => r.json.as[scala.collection.Seq[apidoc.models.Healthcheck]]
          case r => throw new FailedResponse(r)
        }
      }
    }
  
    trait MembershipRequests {
      /**
       * Search all membership requests. Results are always paginated.
       */
      def get(
        orgGuid: scala.Option[java.util.UUID] = None,
        orgKey: scala.Option[String] = None,
        userGuid: scala.Option[java.util.UUID] = None,
        role: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[apidoc.models.MembershipRequest]]
      
      /**
       * Create a membership request
       */
      def post(
        orgGuid: java.util.UUID,
        userGuid: java.util.UUID,
        role: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[apidoc.models.MembershipRequest]
      
      /**
       * Accepts this membership request. User will become a member of the specified
       * organization.
       */
      def postAcceptByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Unit]
      
      /**
       * Declines this membership request. User will NOT become a member of the specified
       * organization.
       */
      def postDeclineByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Unit]
    }
  
    object MembershipRequests extends MembershipRequests {
      override def get(
        orgGuid: scala.Option[java.util.UUID] = None,
        orgKey: scala.Option[String] = None,
        userGuid: scala.Option[java.util.UUID] = None,
        role: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[apidoc.models.MembershipRequest]] = {
        val query = Seq(
          orgGuid.map("org_guid" -> _.toString),
          orgKey.map("org_key" -> _),
          userGuid.map("user_guid" -> _.toString),
          role.map("role" -> _),
          limit.map("limit" -> _.toString),
          offset.map("offset" -> _.toString)
        ).flatten
        
        GET(s"/membership_requests", query).map {
          case r if r.status == 200 => r.json.as[scala.collection.Seq[apidoc.models.MembershipRequest]]
          case r => throw new FailedResponse(r)
        }
      }
      
      override def post(
        orgGuid: java.util.UUID,
        userGuid: java.util.UUID,
        role: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[apidoc.models.MembershipRequest] = {
        val payload = play.api.libs.json.Json.obj(
          "org_guid" -> play.api.libs.json.Json.toJson(orgGuid),
          "user_guid" -> play.api.libs.json.Json.toJson(userGuid),
          "role" -> play.api.libs.json.Json.toJson(role)
        )
        
        POST(s"/membership_requests", payload).map {
          case r if r.status == 200 => r.json.as[apidoc.models.MembershipRequest]
          case r if r.status == 409 => throw new apidoc.error.ErrorsResponse(r)
          case r => throw new FailedResponse(r)
        }
      }
      
      override def postAcceptByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Unit] = {
        POST(s"/membership_requests/${guid}/accept").map {
          case r if r.status == 204 => Unit
          case r if r.status == 409 => throw new apidoc.error.ErrorsResponse(r)
          case r => throw new FailedResponse(r)
        }
      }
      
      override def postDeclineByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Unit] = {
        POST(s"/membership_requests/${guid}/decline").map {
          case r if r.status == 204 => Unit
          case r if r.status == 409 => throw new apidoc.error.ErrorsResponse(r)
          case r => throw new FailedResponse(r)
        }
      }
    }
  
    trait Memberships {
      /**
       * Search all memberships. Results are always paginated.
       */
      def get(
        orgGuid: scala.Option[java.util.UUID] = None,
        orgKey: scala.Option[String] = None,
        userGuid: scala.Option[java.util.UUID] = None,
        role: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[apidoc.models.Membership]]
      
      def getByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Option[apidoc.models.Membership]]
      
      def deleteByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Option[Unit]]
    }
  
    object Memberships extends Memberships {
      override def get(
        orgGuid: scala.Option[java.util.UUID] = None,
        orgKey: scala.Option[String] = None,
        userGuid: scala.Option[java.util.UUID] = None,
        role: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[apidoc.models.Membership]] = {
        val query = Seq(
          orgGuid.map("org_guid" -> _.toString),
          orgKey.map("org_key" -> _),
          userGuid.map("user_guid" -> _.toString),
          role.map("role" -> _),
          limit.map("limit" -> _.toString),
          offset.map("offset" -> _.toString)
        ).flatten
        
        GET(s"/memberships", query).map {
          case r if r.status == 200 => r.json.as[scala.collection.Seq[apidoc.models.Membership]]
          case r => throw new FailedResponse(r)
        }
      }
      
      override def getByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Option[apidoc.models.Membership]] = {
        GET(s"/memberships/${guid}").map {
          case r if r.status == 200 => Some(r.json.as[apidoc.models.Membership])
          case r if r.status == 404 => None
          case r => throw new FailedResponse(r)
        }
      }
      
      override def deleteByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Option[Unit]] = {
        DELETE(s"/memberships/${guid}").map {
          case r if r.status == 204 => Some(Unit)
          case r if r.status == 404 => None
          case r => throw new FailedResponse(r)
        }
      }
    }
  
    trait Organizations {
      /**
       * Search all organizations. Results are always paginated.
       */
      def get(
        guid: scala.Option[java.util.UUID] = None,
        userGuid: scala.Option[java.util.UUID] = None,
        key: scala.Option[String] = None,
        name: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[apidoc.models.Organization]]
      
      /**
       * Create a new organization.
       */
      def post(
        name: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[apidoc.models.Organization]
      
      /**
       * Deletes an organization and all of its associated services.
       */
      def deleteByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Option[Unit]]
    }
  
    object Organizations extends Organizations {
      override def get(
        guid: scala.Option[java.util.UUID] = None,
        userGuid: scala.Option[java.util.UUID] = None,
        key: scala.Option[String] = None,
        name: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[apidoc.models.Organization]] = {
        val query = Seq(
          guid.map("guid" -> _.toString),
          userGuid.map("user_guid" -> _.toString),
          key.map("key" -> _),
          name.map("name" -> _),
          limit.map("limit" -> _.toString),
          offset.map("offset" -> _.toString)
        ).flatten
        
        GET(s"/organizations", query).map {
          case r if r.status == 200 => r.json.as[scala.collection.Seq[apidoc.models.Organization]]
          case r => throw new FailedResponse(r)
        }
      }
      
      override def post(
        name: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[apidoc.models.Organization] = {
        val payload = play.api.libs.json.Json.obj(
          "name" -> play.api.libs.json.Json.toJson(name)
        )
        
        POST(s"/organizations", payload).map {
          case r if r.status == 200 => r.json.as[apidoc.models.Organization]
          case r if r.status == 409 => throw new apidoc.error.ErrorsResponse(r)
          case r => throw new FailedResponse(r)
        }
      }
      
      override def deleteByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Option[Unit]] = {
        DELETE(s"/organizations/${guid}").map {
          case r if r.status == 204 => Some(Unit)
          case r if r.status == 404 => None
          case r => throw new FailedResponse(r)
        }
      }
    }
  
    trait Services {
      /**
       * Search all services. Results are always paginated.
       */
      def getByOrgKey(
        orgKey: String,
        name: scala.Option[String] = None,
        key: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[apidoc.models.Service]]
      
      /**
       * Deletes a specific service and its associated versions.
       */
      def deleteByOrgKeyAndServiceKey(
        orgKey: String,
        serviceKey: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Option[Unit]]
    }
  
    object Services extends Services {
      override def getByOrgKey(
        orgKey: String,
        name: scala.Option[String] = None,
        key: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[apidoc.models.Service]] = {
        val query = Seq(
          name.map("name" -> _),
          key.map("key" -> _),
          limit.map("limit" -> _.toString),
          offset.map("offset" -> _.toString)
        ).flatten
        
        GET(s"/${java.net.URLEncoder.encode(orgKey, "UTF-8")}", query).map {
          case r if r.status == 200 => r.json.as[scala.collection.Seq[apidoc.models.Service]]
          case r => throw new FailedResponse(r)
        }
      }
      
      override def deleteByOrgKeyAndServiceKey(
        orgKey: String,
        serviceKey: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Option[Unit]] = {
        DELETE(s"/${java.net.URLEncoder.encode(orgKey, "UTF-8")}/${java.net.URLEncoder.encode(serviceKey, "UTF-8")}").map {
          case r if r.status == 204 => Some(Unit)
          case r if r.status == 404 => None
          case r => throw new FailedResponse(r)
        }
      }
    }
  
    trait Users {
      /**
       * Search for a specific user. You must specify at least 1 parameter - either a
       * guid, email or token - and will receive back either 0 or 1 users.
       */
      def get(
        guid: scala.Option[java.util.UUID] = None,
        email: scala.Option[String] = None,
        token: scala.Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[apidoc.models.User]]
      
      /**
       * Returns information about the user with this guid.
       */
      def getByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Option[apidoc.models.User]]
      
      /**
       * Used to authenticate a user with an email address and password. Successful
       * authentication returns an instance of the user model. Failed authorizations of
       * any kind are returned as a generic error with code user_authorization_failed.
       */
      def postAuthenticate(
        email: String,
        password: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[apidoc.models.User]
      
      /**
       * Create a new user.
       */
      def post(
        email: String,
        name: scala.Option[String] = None,
        password: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[apidoc.models.User]
      
      /**
       * Updates information about the user with the specified guid.
       */
      def putByGuid(
        guid: java.util.UUID,
        email: String,
        name: scala.Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[apidoc.models.User]
    }
  
    object Users extends Users {
      override def get(
        guid: scala.Option[java.util.UUID] = None,
        email: scala.Option[String] = None,
        token: scala.Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[apidoc.models.User]] = {
        val query = Seq(
          guid.map("guid" -> _.toString),
          email.map("email" -> _),
          token.map("token" -> _)
        ).flatten
        
        GET(s"/users", query).map {
          case r if r.status == 200 => r.json.as[scala.collection.Seq[apidoc.models.User]]
          case r => throw new FailedResponse(r)
        }
      }
      
      override def getByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Option[apidoc.models.User]] = {
        GET(s"/users/${guid}").map {
          case r if r.status == 200 => Some(r.json.as[apidoc.models.User])
          case r if r.status == 404 => None
          case r => throw new FailedResponse(r)
        }
      }
      
      override def postAuthenticate(
        email: String,
        password: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[apidoc.models.User] = {
        val payload = play.api.libs.json.Json.obj(
          "email" -> play.api.libs.json.Json.toJson(email),
          "password" -> play.api.libs.json.Json.toJson(password)
        )
        
        POST(s"/users/authenticate", payload).map {
          case r if r.status == 200 => r.json.as[apidoc.models.User]
          case r if r.status == 409 => throw new apidoc.error.ErrorsResponse(r)
          case r => throw new FailedResponse(r)
        }
      }
      
      override def post(
        email: String,
        name: scala.Option[String] = None,
        password: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[apidoc.models.User] = {
        val payload = play.api.libs.json.Json.obj(
          "email" -> play.api.libs.json.Json.toJson(email),
          "name" -> play.api.libs.json.Json.toJson(name),
          "password" -> play.api.libs.json.Json.toJson(password)
        )
        
        POST(s"/users", payload).map {
          case r if r.status == 200 => r.json.as[apidoc.models.User]
          case r if r.status == 409 => throw new apidoc.error.ErrorsResponse(r)
          case r => throw new FailedResponse(r)
        }
      }
      
      override def putByGuid(
        guid: java.util.UUID,
        email: String,
        name: scala.Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[apidoc.models.User] = {
        val payload = play.api.libs.json.Json.obj(
          "email" -> play.api.libs.json.Json.toJson(email),
          "name" -> play.api.libs.json.Json.toJson(name)
        )
        
        PUT(s"/users/${guid}", payload).map {
          case r if r.status == 200 => r.json.as[apidoc.models.User]
          case r if r.status == 409 => throw new apidoc.error.ErrorsResponse(r)
          case r => throw new FailedResponse(r)
        }
      }
    }
  
    trait Versions {
      /**
       * Search all versions of this service. Results are always paginated.
       */
      def getByOrgKeyAndServiceKey(
        orgKey: String,
        serviceKey: String,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[apidoc.models.Version]]
      
      /**
       * Retrieve a specific version of a service.
       */
      def getByOrgKeyAndServiceKeyAndVersion(
        orgKey: String,
        serviceKey: String,
        version: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Option[apidoc.models.Version]]
      
      /**
       * Create or update the service with the specified version.
       */
      def putByOrgKeyAndServiceKeyAndVersion(
        orgKey: String,
        serviceKey: String,
        version: String,
        json: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[apidoc.models.Version]
      
      /**
       * Deletes a specific version.
       */
      def deleteByOrgKeyAndServiceKeyAndVersion(
        orgKey: String,
        serviceKey: String,
        version: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Option[Unit]]
    }
  
    object Versions extends Versions {
      override def getByOrgKeyAndServiceKey(
        orgKey: String,
        serviceKey: String,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[apidoc.models.Version]] = {
        val query = Seq(
          limit.map("limit" -> _.toString),
          offset.map("offset" -> _.toString)
        ).flatten
        
        GET(s"/${java.net.URLEncoder.encode(orgKey, "UTF-8")}/${java.net.URLEncoder.encode(serviceKey, "UTF-8")}", query).map {
          case r if r.status == 200 => r.json.as[scala.collection.Seq[apidoc.models.Version]]
          case r => throw new FailedResponse(r)
        }
      }
      
      override def getByOrgKeyAndServiceKeyAndVersion(
        orgKey: String,
        serviceKey: String,
        version: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Option[apidoc.models.Version]] = {
        GET(s"/${java.net.URLEncoder.encode(orgKey, "UTF-8")}/${java.net.URLEncoder.encode(serviceKey, "UTF-8")}/${java.net.URLEncoder.encode(version, "UTF-8")}").map {
          case r if r.status == 200 => Some(r.json.as[apidoc.models.Version])
          case r if r.status == 404 => None
          case r => throw new FailedResponse(r)
        }
      }
      
      override def putByOrgKeyAndServiceKeyAndVersion(
        orgKey: String,
        serviceKey: String,
        version: String,
        json: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[apidoc.models.Version] = {
        val payload = play.api.libs.json.Json.obj(
          "json" -> play.api.libs.json.Json.toJson(json)
        )
        
        PUT(s"/${java.net.URLEncoder.encode(orgKey, "UTF-8")}/${java.net.URLEncoder.encode(serviceKey, "UTF-8")}/${java.net.URLEncoder.encode(version, "UTF-8")}", payload).map {
          case r if r.status == 200 => r.json.as[apidoc.models.Version]
          case r => throw new FailedResponse(r)
        }
      }
      
      override def deleteByOrgKeyAndServiceKeyAndVersion(
        orgKey: String,
        serviceKey: String,
        version: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Option[Unit]] = {
        DELETE(s"/${java.net.URLEncoder.encode(orgKey, "UTF-8")}/${java.net.URLEncoder.encode(serviceKey, "UTF-8")}/${java.net.URLEncoder.encode(version, "UTF-8")}").map {
          case r if r.status == 204 => Some(Unit)
          case r if r.status == 404 => None
          case r => throw new FailedResponse(r)
        }
      }
    }
  }
}
