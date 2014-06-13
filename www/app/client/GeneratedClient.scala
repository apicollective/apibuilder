package apidoc.models {
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
    def imageUrl: scala.Option[String]
  }

  case class UserImpl(
    guid: java.util.UUID,
    email: String,
    name: String,
    imageUrl: scala.Option[String] = None
  ) extends User

  object User {
    def apply(guid: java.util.UUID, email: String, name: String, imageUrl: scala.Option[String]): UserImpl = {
      new UserImpl(guid,email,name,imageUrl)
    }
  
    def unapply(x: User) = {
      Some(x.guid, x.email, x.name, x.imageUrl)
    }
  
    import scala.language.implicitConversions
    implicit def toImpl(x: User): UserImpl = x match {
      case impl: UserImpl => impl
      case _ => new UserImpl(x.guid,x.email,x.name,x.imageUrl)
    }
  }
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

  object Organization {
    def apply(guid: java.util.UUID, key: String, name: String): OrganizationImpl = {
      new OrganizationImpl(guid,key,name)
    }
  
    def unapply(x: Organization) = {
      Some(x.guid, x.key, x.name)
    }
  
    import scala.language.implicitConversions
    implicit def toImpl(x: Organization): OrganizationImpl = x match {
      case impl: OrganizationImpl => impl
      case _ => new OrganizationImpl(x.guid,x.key,x.name)
    }
  }
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

  object Membership {
    def apply(guid: java.util.UUID, user: User, organization: Organization, role: String): MembershipImpl = {
      new MembershipImpl(guid,user,organization,role)
    }
  
    def unapply(x: Membership) = {
      Some(x.guid, x.user, x.organization, x.role)
    }
  
    import scala.language.implicitConversions
    implicit def toImpl(x: Membership): MembershipImpl = x match {
      case impl: MembershipImpl => impl
      case _ => new MembershipImpl(x.guid,x.user,x.organization,x.role)
    }
  }
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

  object MembershipRequest {
    def apply(guid: java.util.UUID, user: User, organization: Organization, role: String): MembershipRequestImpl = {
      new MembershipRequestImpl(guid,user,organization,role)
    }
  
    def unapply(x: MembershipRequest) = {
      Some(x.guid, x.user, x.organization, x.role)
    }
  
    import scala.language.implicitConversions
    implicit def toImpl(x: MembershipRequest): MembershipRequestImpl = x match {
      case impl: MembershipRequestImpl => impl
      case _ => new MembershipRequestImpl(x.guid,x.user,x.organization,x.role)
    }
  }
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
    
    def description: scala.Option[String]
  }

  case class ServiceImpl(
    guid: java.util.UUID,
    name: String,
    key: String,
    description: scala.Option[String] = None
  ) extends Service

  object Service {
    def apply(guid: java.util.UUID, name: String, key: String, description: scala.Option[String]): ServiceImpl = {
      new ServiceImpl(guid,name,key,description)
    }
  
    def unapply(x: Service) = {
      Some(x.guid, x.name, x.key, x.description)
    }
  
    import scala.language.implicitConversions
    implicit def toImpl(x: Service): ServiceImpl = x match {
      case impl: ServiceImpl => impl
      case _ => new ServiceImpl(x.guid,x.name,x.key,x.description)
    }
  }
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

  object Version {
    def apply(guid: java.util.UUID, version: String, json: String): VersionImpl = {
      new VersionImpl(guid,version,json)
    }
  
    def unapply(x: Version) = {
      Some(x.guid, x.version, x.json)
    }
  
    import scala.language.implicitConversions
    implicit def toImpl(x: Version): VersionImpl = x match {
      case impl: VersionImpl => impl
      case _ => new VersionImpl(x.guid,x.version,x.json)
    }
  }
  /**
   * Generated source code.
   */
  trait Code {
    def version: Version
    
    /**
     * The target platform.
     */
    def target: String
    
    /**
     * The actual source code.
     */
    def source: String
  }

  case class CodeImpl(
    version: Version,
    target: String,
    source: String
  ) extends Code

  object Code {
    def apply(version: Version, target: String, source: String): CodeImpl = {
      new CodeImpl(version,target,source)
    }
  
    def unapply(x: Code) = {
      Some(x.version, x.target, x.source)
    }
  
    import scala.language.implicitConversions
    implicit def toImpl(x: Code): CodeImpl = x match {
      case impl: CodeImpl => impl
      case _ => new CodeImpl(x.version,x.target,x.source)
    }
  }
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

  object Error {
    def apply(code: String, message: String): ErrorImpl = {
      new ErrorImpl(code,message)
    }
  
    def unapply(x: Error) = {
      Some(x.code, x.message)
    }
  
    import scala.language.implicitConversions
    implicit def toImpl(x: Error): ErrorImpl = x match {
      case impl: ErrorImpl => impl
      case _ => new ErrorImpl(x.code,x.message)
    }
  }
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

    implicit val readsUser: play.api.libs.json.Reads[User] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "email").read[String] and
         (__ \ "name").read[String] and
         (__ \ "image_url").readNullable[String])(UserImpl.apply _)
      }
    
    implicit val writesUser: play.api.libs.json.Writes[User] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "email").write[String] and
         (__ \ "name").write[String] and
         (__ \ "image_url").write[scala.Option[String]])(unlift(User.unapply))
      }
    
    implicit val readsOrganization: play.api.libs.json.Reads[Organization] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "key").read[String] and
         (__ \ "name").read[String])(OrganizationImpl.apply _)
      }
    
    implicit val writesOrganization: play.api.libs.json.Writes[Organization] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "key").write[String] and
         (__ \ "name").write[String])(unlift(Organization.unapply))
      }
    
    implicit val readsMembership: play.api.libs.json.Reads[Membership] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "user").read[User] and
         (__ \ "organization").read[Organization] and
         (__ \ "role").read[String])(MembershipImpl.apply _)
      }
    
    implicit val writesMembership: play.api.libs.json.Writes[Membership] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "user").write[User] and
         (__ \ "organization").write[Organization] and
         (__ \ "role").write[String])(unlift(Membership.unapply))
      }
    
    implicit val readsMembershipRequest: play.api.libs.json.Reads[MembershipRequest] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "user").read[User] and
         (__ \ "organization").read[Organization] and
         (__ \ "role").read[String])(MembershipRequestImpl.apply _)
      }
    
    implicit val writesMembershipRequest: play.api.libs.json.Writes[MembershipRequest] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "user").write[User] and
         (__ \ "organization").write[Organization] and
         (__ \ "role").write[String])(unlift(MembershipRequest.unapply))
      }
    
    implicit val readsService: play.api.libs.json.Reads[Service] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "name").read[String] and
         (__ \ "key").read[String] and
         (__ \ "description").readNullable[String])(ServiceImpl.apply _)
      }
    
    implicit val writesService: play.api.libs.json.Writes[Service] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "name").write[String] and
         (__ \ "key").write[String] and
         (__ \ "description").write[scala.Option[String]])(unlift(Service.unapply))
      }
    
    implicit val readsVersion: play.api.libs.json.Reads[Version] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "version").read[String] and
         (__ \ "json").read[String])(VersionImpl.apply _)
      }
    
    implicit val writesVersion: play.api.libs.json.Writes[Version] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "version").write[String] and
         (__ \ "json").write[String])(unlift(Version.unapply))
      }
    
    implicit val readsCode: play.api.libs.json.Reads[Code] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "version").read[Version] and
         (__ \ "target").read[String] and
         (__ \ "source").read[String])(CodeImpl.apply _)
      }
    
    implicit val writesCode: play.api.libs.json.Writes[Code] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "version").write[Version] and
         (__ \ "target").write[String] and
         (__ \ "source").write[String])(unlift(Code.unapply))
      }
    
    implicit val readsError: play.api.libs.json.Reads[Error] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "code").read[String] and
         (__ \ "message").read[String])(ErrorImpl.apply _)
      }
    
    implicit val writesError: play.api.libs.json.Writes[Error] =
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

    object Code {
      /**
       * Generate code for a specific version of a service.
       */
      def getByVersionAndTarget(
        version: String,
        target: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Code]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        
        
        GET(s"/code/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(version)}/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(target)}", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[Code], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
    
    object MembershipRequests {
      /**
       * Search all membership requests. Results are always paginated.
       */
      def get(
        organizationGuid: scala.Option[java.util.UUID] = None,
        organizationKey: scala.Option[java.util.UUID] = None,
        userGuid: scala.Option[java.util.UUID] = None,
        role: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.immutable.Seq[MembershipRequest]]] = {
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
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.immutable.Seq[MembershipRequest]], 200)
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
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.immutable.Seq[Error]], 409)
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
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.immutable.Seq[Error]], 409)
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
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.immutable.Seq[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
    
    object Memberships {
      /**
       * Search all memberships. Results are always paginated.
       */
      def get(
        organizationGuid: scala.Option[java.util.UUID] = None,
        organizationKey: scala.Option[java.util.UUID] = None,
        userGuid: scala.Option[java.util.UUID] = None,
        role: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.immutable.Seq[Membership]]] = {
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
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.immutable.Seq[Membership]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
    
    object Organizations {
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
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.immutable.Seq[Organization]]] = {
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
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.immutable.Seq[Organization]], 200)
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
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.immutable.Seq[Error]], 409)
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
        name: scala.Option[String] = None,
        key: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.immutable.Seq[Service]]] = {
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
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.immutable.Seq[Service]], 200)
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
        guid: scala.Option[java.util.UUID] = None,
        email: scala.Option[String] = None,
        token: scala.Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.immutable.Seq[User]]] = {
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
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.immutable.Seq[User]], 200)
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
        name: scala.Option[String] = None,
        imageUrl: scala.Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[User]] = {
        val payload = play.api.libs.json.Json.obj(
          "email" -> play.api.libs.json.Json.toJson(email),
          "name" -> play.api.libs.json.Json.toJson(name),
          "image_url" -> play.api.libs.json.Json.toJson(imageUrl)
        )
        
        POST(s"/users", payload).map {
          case r if r.status == 201 => new ResponseImpl(r.json.as[User], 201)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.immutable.Seq[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Updates information about the user with the specified guid.
       */
      def putByGuid(
        guid: String,
        email: String,
        name: scala.Option[String] = None,
        imageUrl: scala.Option[String] = None
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
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.immutable.Seq[Error]], 409)
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
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.immutable.Seq[Version]]] = {
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
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.immutable.Seq[Version]], 200)
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
