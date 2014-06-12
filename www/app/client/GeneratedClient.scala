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
  }

  import json._

  /**
   * A user is a top level person interacting with the api doc server.
   */
  trait User {
    /**
     * Internal unique identifier for this user.
     */
    def guid: java.util.UUID
    
    def email: String
    
    def name: String
    
    /**
     * Image avatar for this user
     */
    def imageUrl: Option[String]
  }
  
  case class UserImpl(
    guid: java.util.UUID,
    email: String,
    name: String,
    imageUrl: Option[String] = None
  ) extends User
  
  /**
   * An organization is used to group a set of services together.
   */
  trait Organization {
    /**
     * Internal unique identifier for this organization.
     */
    def guid: java.util.UUID
    
    /**
     * Used as a unique key in the URL path. Key is automatically derived from the
     * organization name.
     */
    def key: String
    
    /**
     * The name of this organization.
     */
    def name: String
  }
  
  case class OrganizationImpl(
    guid: java.util.UUID,
    key: String,
    name: String
  ) extends Organization
  
  /**
   * A membership represents a user in a specific role to an organization.
   * Memberships cannot be created directly. Instead you first create a membership
   * request, then that request is either accepted or declined.
   */
  trait Membership {
    /**
     * Internal unique identifier for this membership.
     */
    def guid: java.util.UUID
    
    def user: User
    
    def organization: Organization
    
    /**
     * The role this user plays for this organization. Typically member or admin.
     */
    def role: String
  }
  
  case class MembershipImpl(
    guid: java.util.UUID,
    user: User,
    organization: Organization,
    role: String
  ) extends Membership
  
  /**
   * A membership request represents a user requesting to join an organization with a
   * specificed role (e.g. as a member or an admin). Membership requests can be
   * reviewed by any current admin of the organization who can either accept or
   * decline the request.
   */
  trait MembershipRequest {
    /**
     * Internal unique identifier for this membership request.
     */
    def guid: java.util.UUID
    
    def user: User
    
    def organization: Organization
    
    /**
     * The requested role for membership to this organization. Typically member or
     * admin.
     */
    def role: String
  }
  
  case class MembershipRequestImpl(
    guid: java.util.UUID,
    user: User,
    organization: Organization,
    role: String
  ) extends MembershipRequest
  
  /**
   * A service has a name and multiple versions of an API (Interface).
   */
  trait Service {
    /**
     * Internal unique identifier for this service.
     */
    def guid: java.util.UUID
    
    /**
     * The unique name for this service.
     */
    def name: String
    
    /**
     * Used as a unique key in the URL path. Key is automatically derived from the
     * service name.
     */
    def key: String
    
    def description: Option[String]
  }
  
  case class ServiceImpl(
    guid: java.util.UUID,
    name: String,
    key: String,
    description: Option[String] = None
  ) extends Service
  
  /**
   * Represents a unique version of the service.
   */
  trait Version {
    /**
     * Internal unique identifier for this version.
     */
    def guid: java.util.UUID
    
    /**
     * The tag for this version. Can be anything, but if semver style version number is
     * used, we automatically correctly sort by version number to find latest.
     * Otherwise latest version is considered to be the most recently created.
     */
    def version: String
    
    /**
     * JSON description of the service.
     */
    def json: String
  }
  
  case class VersionImpl(
    guid: java.util.UUID,
    version: String,
    json: String
  ) extends Version
  
  trait Error {
    /**
     * Machine readable code for this specific error message
     */
    def code: String
    
    /**
     * Description of the error
     */
    def message: String
  }
  
  case class ErrorImpl(
    code: String,
    message: String
  ) extends Error

  object User {
    def unapply(x: User) = {
      Some(x.guid, x.email, x.name, x.imageUrl)
    }
  
    implicit val reads: play.api.libs.json.Reads[User] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "email").read[String] and
         (__ \ "name").read[String] and
         (__ \ "image_url").readNullable[String])(UserImpl.apply _)
      }
  
    implicit val writes: play.api.libs.json.Writes[User] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "email").write[String] and
         (__ \ "name").write[String] and
         (__ \ "image_url").write[Option[String]])(unlift(User.unapply))
      }
  }
  
  object Organization {
    def unapply(x: Organization) = {
      Some(x.guid, x.key, x.name)
    }
  
    implicit val reads: play.api.libs.json.Reads[Organization] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "key").read[String] and
         (__ \ "name").read[String])(OrganizationImpl.apply _)
      }
  
    implicit val writes: play.api.libs.json.Writes[Organization] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "key").write[String] and
         (__ \ "name").write[String])(unlift(Organization.unapply))
      }
  }
  
  object Membership {
    def unapply(x: Membership) = {
      Some(x.guid, x.user, x.organization, x.role)
    }
  
    implicit val reads: play.api.libs.json.Reads[Membership] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "user").read[User] and
         (__ \ "organization").read[Organization] and
         (__ \ "role").read[String])(MembershipImpl.apply _)
      }
  
    implicit val writes: play.api.libs.json.Writes[Membership] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "user").write[User] and
         (__ \ "organization").write[Organization] and
         (__ \ "role").write[String])(unlift(Membership.unapply))
      }
  }
  
  object MembershipRequest {
    def unapply(x: MembershipRequest) = {
      Some(x.guid, x.user, x.organization, x.role)
    }
  
    implicit val reads: play.api.libs.json.Reads[MembershipRequest] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "user").read[User] and
         (__ \ "organization").read[Organization] and
         (__ \ "role").read[String])(MembershipRequestImpl.apply _)
      }
  
    implicit val writes: play.api.libs.json.Writes[MembershipRequest] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "user").write[User] and
         (__ \ "organization").write[Organization] and
         (__ \ "role").write[String])(unlift(MembershipRequest.unapply))
      }
  }
  
  object Service {
    def unapply(x: Service) = {
      Some(x.guid, x.name, x.key, x.description)
    }
  
    implicit val reads: play.api.libs.json.Reads[Service] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "name").read[String] and
         (__ \ "key").read[String] and
         (__ \ "description").readNullable[String])(ServiceImpl.apply _)
      }
  
    implicit val writes: play.api.libs.json.Writes[Service] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "name").write[String] and
         (__ \ "key").write[String] and
         (__ \ "description").write[Option[String]])(unlift(Service.unapply))
      }
  }
  
  object Version {
    def unapply(x: Version) = {
      Some(x.guid, x.version, x.json)
    }
  
    implicit val reads: play.api.libs.json.Reads[Version] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "version").read[String] and
         (__ \ "json").read[String])(VersionImpl.apply _)
      }
  
    implicit val writes: play.api.libs.json.Writes[Version] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "version").write[String] and
         (__ \ "json").write[String])(unlift(Version.unapply))
      }
  }
  
  object Error {
    def unapply(x: Error) = {
      Some(x.code, x.message)
    }
  
    implicit val reads: play.api.libs.json.Reads[Error] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "code").read[String] and
         (__ \ "message").read[String])(ErrorImpl.apply _)
      }
  
    implicit val writes: play.api.libs.json.Writes[Error] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "code").write[String] and
         (__ \ "message").write[String])(unlift(Error.unapply))
      }
  }
}

package apidoc {
  class Client(apiUrl: String, apiToken: Option[String] = None) {
    import apidoc.models._
    import apidoc.models.json._

    private val logger = play.api.Logger("apidoc.client")

    logger.info(s"Initializing apidoc.client for url $apiUrl")

    private def requestHolder(path: String) = {
      import play.api.Play.current

      val url = apiUrl + path
      val holder = play.api.libs.ws.WS.url(url)
      apiToken.map { token =>
        holder.withAuth(token, "", play.api.libs.ws.WSAuthScheme.BASIC)
      }.getOrElse {
        holder
      }
    }

    private def logRequest(method: String, req: play.api.libs.ws.WSRequestHolder)(implicit ec: scala.concurrent.ExecutionContext): play.api.libs.ws.WSRequestHolder = {
      val q = req.queryString.flatMap { case (name, values) =>
        values.map(name -> _).map { case (name, value) =>
          s"$name=$value"
        }
      }.mkString("&")
      val url = s"${req.url}?$q"
      apiToken.map { _ =>
        logger.info(s"curl -X $method -u '[REDACTED]:' $url")
      }.getOrElse {
        logger.info(s"curl -X $method $url")
      }
      req
    }

    private def processResponse(f: scala.concurrent.Future[play.api.libs.ws.WSResponse])(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      f.map { response =>
        lazy val body: String = scala.util.Try {
          play.api.libs.json.Json.prettyPrint(response.json)
        } getOrElse {
          response.body
        }
        logger.debug(s"${response.status} -> $body")
        response
      }
    }

    private def POST(path: String, data: play.api.libs.json.JsValue)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      processResponse(logRequest("POST", requestHolder(path)).post(data))
    }

    private def GET(path: String, q: Seq[(String, String)])(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      processResponse(logRequest("GET", requestHolder(path).withQueryString(q:_*)).get())
    }

    private def PUT(path: String, data: play.api.libs.json.JsValue)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      processResponse(logRequest("PUT", requestHolder(path)).put(data))
    }

    private def PATCH(path: String, data: play.api.libs.json.JsValue)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      processResponse(logRequest("PATCH", requestHolder(path)).patch(data))
    }

    private def DELETE(path: String)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      processResponse(logRequest("DELETE", requestHolder(path)).delete())
    }

    trait Response[T] {
      val entity: T
      val status: Int
    }

    object Response {
      def unapply[T](r: Response[T]) = Some((r.entity, r.status))
    }

    case class ResponseImpl[T](entity: T, status: Int) extends Response[T]

    case class FailedResponse[T](entity: T, status: Int)
      extends Exception(s"request failed with status[$status]: ${entity}")
      with Response[T]

    object MembershipRequests {
      /**
       * Search all membership requests. Results are always paginated.
       */
      def get(
        organizationGuid: Option[java.util.UUID] = None,
        organizationKey: Option[java.util.UUID] = None,
        userGuid: Option[java.util.UUID] = None,
        role: Option[String] = None,
        limit: Option[Int] = None,
        offset: Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.immutable.List[MembershipRequest]]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        queryBuilder ++= organizationGuid.map { x =>
          "organization_guid" -> (
            { x: java.util.UUID =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= organizationKey.map { x =>
          "organization_key" -> (
            { x: java.util.UUID =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= userGuid.map { x =>
          "user_guid" -> (
            { x: java.util.UUID =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= role.map { x =>
          "role" -> (
            { x: String =>
              x
            }
          )(x)
        }
        queryBuilder ++= limit.map { x =>
          "limit" -> (
            { x: Int =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= offset.map { x =>
          "offset" -> (
            { x: Int =>
              x.toString
            }
          )(x)
        }
        
        GET(s"/membership_requests", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.immutable.List[MembershipRequest]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Create a membership request
       */
      def post(
        organizationGuid: java.util.UUID,
        userGuid: java.util.UUID,
        role: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[MembershipRequest]] = {
        val payload = play.api.libs.json.Json.obj(
          "organization_guid" -> play.api.libs.json.Json.toJson(organizationGuid),
          "user_guid" -> play.api.libs.json.Json.toJson(userGuid),
          "role" -> play.api.libs.json.Json.toJson(role)
        )
        
        POST(s"/membership_requests", payload).map {
          case r if r.status == 201 => new ResponseImpl(r.json.as[MembershipRequest], 201)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.immutable.List[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Accepts this membership request. User will become a member of the specified
       * organization.
       */
      def postAcceptByGuid(
        guid: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Unit]] = {
        val payload = play.api.libs.json.Json.obj(
          
        )
        
        POST(s"/membership_requests/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(guid)}/accept", payload).map {
          case r if r.status == 204 => new ResponseImpl((), 204)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.immutable.List[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Declines this membership request. User will NOT become a member of the specified
       * organization.
       */
      def postDeclineByGuid(
        guid: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Unit]] = {
        val payload = play.api.libs.json.Json.obj(
          
        )
        
        POST(s"/membership_requests/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(guid)}/decline", payload).map {
          case r if r.status == 204 => new ResponseImpl((), 204)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.immutable.List[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
    
    object Memberships {
      /**
       * Search all memberships. Results are always paginated.
       */
      def get(
        organizationGuid: Option[java.util.UUID] = None,
        organizationKey: Option[java.util.UUID] = None,
        userGuid: Option[java.util.UUID] = None,
        role: Option[String] = None,
        limit: Option[Int] = None,
        offset: Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.immutable.List[Membership]]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        queryBuilder ++= organizationGuid.map { x =>
          "organization_guid" -> (
            { x: java.util.UUID =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= organizationKey.map { x =>
          "organization_key" -> (
            { x: java.util.UUID =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= userGuid.map { x =>
          "user_guid" -> (
            { x: java.util.UUID =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= role.map { x =>
          "role" -> (
            { x: String =>
              x
            }
          )(x)
        }
        queryBuilder ++= limit.map { x =>
          "limit" -> (
            { x: Int =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= offset.map { x =>
          "offset" -> (
            { x: Int =>
              x.toString
            }
          )(x)
        }
        
        GET(s"/memberships", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.immutable.List[Membership]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
    
    object Organizations {
      /**
       * Search all organizations. Results are always paginated.
       */
      def get(
        guid: Option[java.util.UUID] = None,
        userGuid: Option[java.util.UUID] = None,
        key: Option[String] = None,
        name: Option[String] = None,
        limit: Option[Int] = None,
        offset: Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.immutable.List[Organization]]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        queryBuilder ++= guid.map { x =>
          "guid" -> (
            { x: java.util.UUID =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= userGuid.map { x =>
          "user_guid" -> (
            { x: java.util.UUID =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= key.map { x =>
          "key" -> (
            { x: String =>
              x
            }
          )(x)
        }
        queryBuilder ++= name.map { x =>
          "name" -> (
            { x: String =>
              x
            }
          )(x)
        }
        queryBuilder ++= limit.map { x =>
          "limit" -> (
            { x: Int =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= offset.map { x =>
          "offset" -> (
            { x: Int =>
              x.toString
            }
          )(x)
        }
        
        GET(s"/organizations", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.immutable.List[Organization]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Create a new organization.
       */
      def post(
        name: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Organization]] = {
        val payload = play.api.libs.json.Json.obj(
          "name" -> play.api.libs.json.Json.toJson(name)
        )
        
        POST(s"/organizations", payload).map {
          case r if r.status == 201 => new ResponseImpl(r.json.as[Organization], 201)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.immutable.List[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Deletes an organization and all of its associated services.
       */
      def deleteByGuid(
        guid: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Unit]] = {
        DELETE(s"/organizations/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(guid)}").map {
          case r if r.status == 204 => new ResponseImpl((), 204)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
    
    object Services {
      /**
       * Search all services. Results are always paginated.
       */
      def getByOrgKey(
        orgKey: String,
        name: Option[String] = None,
        key: Option[String] = None,
        limit: Option[Int] = None,
        offset: Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.immutable.List[Service]]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        queryBuilder ++= name.map { x =>
          "name" -> (
            { x: String =>
              x
            }
          )(x)
        }
        queryBuilder ++= key.map { x =>
          "key" -> (
            { x: String =>
              x
            }
          )(x)
        }
        queryBuilder ++= limit.map { x =>
          "limit" -> (
            { x: Int =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= offset.map { x =>
          "offset" -> (
            { x: Int =>
              x.toString
            }
          )(x)
        }
        
        GET(s"/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(orgKey)}", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.immutable.List[Service]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Deletes a specific service and its associated versions.
       */
      def deleteByOrgKeyAndServiceKey(
        orgKey: String,
        serviceKey: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Unit]] = {
        DELETE(s"/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(orgKey)}/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(serviceKey)}").map {
          case r if r.status == 204 => new ResponseImpl((), 204)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
    
    object Users {
      /**
       * Search for a specific user. You must specify at least 1 parameter - either a
       * guid, email or token - and will receive back either 0 or 1 users.
       */
      def get(
        guid: Option[java.util.UUID] = None,
        email: Option[String] = None,
        token: Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.immutable.List[User]]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        queryBuilder ++= guid.map { x =>
          "guid" -> (
            { x: java.util.UUID =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= email.map { x =>
          "email" -> (
            { x: String =>
              x
            }
          )(x)
        }
        queryBuilder ++= token.map { x =>
          "token" -> (
            { x: String =>
              x
            }
          )(x)
        }
        
        GET(s"/users", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.immutable.List[User]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Returns information about the user with this guid.
       */
      def getByGuid(
        guid: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[User]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        
        
        GET(s"/users/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(guid)}", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[User], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Create a new user.
       */
      def post(
        email: String,
        name: Option[String] = None,
        imageUrl: Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[User]] = {
        val payload = play.api.libs.json.Json.obj(
          "email" -> play.api.libs.json.Json.toJson(email),
          "name" -> play.api.libs.json.Json.toJson(name),
          "image_url" -> play.api.libs.json.Json.toJson(imageUrl)
        )
        
        POST(s"/users", payload).map {
          case r if r.status == 201 => new ResponseImpl(r.json.as[User], 201)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.immutable.List[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Updates information about the user with the specified guid.
       */
      def putByGuid(
        guid: String,
        email: String,
        name: Option[String] = None,
        imageUrl: Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[User]] = {
        val payload = play.api.libs.json.Json.obj(
          "email" -> play.api.libs.json.Json.toJson(email),
          "name" -> play.api.libs.json.Json.toJson(name),
          "image_url" -> play.api.libs.json.Json.toJson(imageUrl)
        )
        
        PUT(s"/users/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(guid)}", payload).map {
          case r if r.status == 201 => new ResponseImpl(r.json.as[User], 201)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.immutable.List[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
    
    object Versions {
      /**
       * Search all versions of this service. Results are always paginated.
       */
      def getByOrgKeyAndServiceKey(
        orgKey: String,
        serviceKey: String,
        limit: Option[Int] = None,
        offset: Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.immutable.List[Version]]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        queryBuilder ++= limit.map { x =>
          "limit" -> (
            { x: Int =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= offset.map { x =>
          "offset" -> (
            { x: Int =>
              x.toString
            }
          )(x)
        }
        
        GET(s"/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(orgKey)}/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(serviceKey)}", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.immutable.List[Version]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Retrieve a specific version of a service.
       */
      def getByOrgKeyAndServiceKeyAndVersion(
        orgKey: String,
        serviceKey: String,
        version: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Version]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        
        
        GET(s"/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(orgKey)}/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(serviceKey)}/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(version)}", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[Version], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Create or update the service with the specified version.
       */
      def putByOrgKeyAndServiceKeyAndVersion(
        orgKey: String,
        serviceKey: String,
        version: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Version]] = {
        val payload = play.api.libs.json.Json.obj(
          
        )
        
        PUT(s"/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(orgKey)}/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(serviceKey)}/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(version)}", payload).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[Version], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Deletes a specific version.
       */
      def deleteByOrgKeyAndServiceKeyAndVersion(
        orgKey: String,
        serviceKey: String,
        version: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Unit]] = {
        DELETE(s"/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(orgKey)}/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(serviceKey)}/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(version)}").map {
          case r if r.status == 204 => new ResponseImpl((), 204)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
  }
}
