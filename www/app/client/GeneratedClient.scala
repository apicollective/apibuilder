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

    object Code {
      /**
       * Generate code for a specific version of a service.
       */
      def getByVersionGuidAndTargetName(
        versionGuid: java.util.UUID,
        targetName: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Code]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        
        
        GET(s"/code/${({x: java.util.UUID =>
          val s = x.toString
          java.net.URLEncoder.encode(s, "UTF-8")
        })(versionGuid)}/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(targetName)}", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[Code], 200)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.Seq[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
    
    object Healthchecks {
      def get(
      
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.Seq[Healthcheck]]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        
        
        GET(s"/_internal_/healthcheck", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.Seq[Healthcheck]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
    
    object MembershipRequests {
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
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.Seq[MembershipRequest]]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        queryBuilder ++= orgGuid.map { x =>
          "org_guid" -> (
            { x: java.util.UUID =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= orgKey.map { x =>
          "org_key" -> (
            { x: String =>
              x
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
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.Seq[MembershipRequest]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Create a membership request
       */
      def post(
        orgGuid: java.util.UUID,
        userGuid: java.util.UUID,
        role: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[MembershipRequest]] = {
        val payload = play.api.libs.json.Json.obj(
          "org_guid" -> play.api.libs.json.Json.toJson(orgGuid),
          "user_guid" -> play.api.libs.json.Json.toJson(userGuid),
          "role" -> play.api.libs.json.Json.toJson(role)
        )
        
        POST(s"/membership_requests", payload).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[MembershipRequest], 200)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.Seq[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Accepts this membership request. User will become a member of the specified
       * organization.
       */
      def postAcceptByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Unit]] = {
        val payload = play.api.libs.json.Json.obj(
          
        )
        
        POST(s"/membership_requests/${({x: java.util.UUID =>
          val s = x.toString
          java.net.URLEncoder.encode(s, "UTF-8")
        })(guid)}/accept", payload).map {
          case r if r.status == 204 => new ResponseImpl((), 204)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.Seq[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Declines this membership request. User will NOT become a member of the specified
       * organization.
       */
      def postDeclineByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Unit]] = {
        val payload = play.api.libs.json.Json.obj(
          
        )
        
        POST(s"/membership_requests/${({x: java.util.UUID =>
          val s = x.toString
          java.net.URLEncoder.encode(s, "UTF-8")
        })(guid)}/decline", payload).map {
          case r if r.status == 204 => new ResponseImpl((), 204)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.Seq[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
    
    object Memberships {
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
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.Seq[Membership]]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        queryBuilder ++= orgGuid.map { x =>
          "org_guid" -> (
            { x: java.util.UUID =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= orgKey.map { x =>
          "org_key" -> (
            { x: String =>
              x
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
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.Seq[Membership]], 200)
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
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.Seq[Organization]]] = {
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
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.Seq[Organization]], 200)
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
          case r if r.status == 200 => new ResponseImpl(r.json.as[Organization], 200)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.Seq[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Deletes an organization and all of its associated services.
       */
      def deleteByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Unit]] = {
        DELETE(s"/organizations/${({x: java.util.UUID =>
          val s = x.toString
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
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.Seq[Service]]] = {
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
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.Seq[Service]], 200)
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
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.Seq[User]]] = {
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
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.Seq[User]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Returns information about the user with this guid.
       */
      def getByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[User]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        
        
        GET(s"/users/${({x: java.util.UUID =>
          val s = x.toString
          java.net.URLEncoder.encode(s, "UTF-8")
        })(guid)}", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[User], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Used to authenticate a user with an email address and password. Successful
       * authentication returns an instance of the user model. Failed authorizations of
       * any kind are returned as a generic error with code user_authorization_failed.
       */
      def postAuthenticate(
        email: String,
        password: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[User]] = {
        val payload = play.api.libs.json.Json.obj(
          "email" -> play.api.libs.json.Json.toJson(email),
          "password" -> play.api.libs.json.Json.toJson(password)
        )
        
        POST(s"/users/authenticate", payload).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[User], 200)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.Seq[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Create a new user.
       */
      def post(
        email: String,
        name: scala.Option[String] = None,
        password: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[User]] = {
        val payload = play.api.libs.json.Json.obj(
          "email" -> play.api.libs.json.Json.toJson(email),
          "name" -> play.api.libs.json.Json.toJson(name),
          "password" -> play.api.libs.json.Json.toJson(password)
        )
        
        POST(s"/users", payload).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[User], 200)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.Seq[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      /**
       * Updates information about the user with the specified guid.
       */
      def putByGuid(
        guid: java.util.UUID,
        email: String,
        name: scala.Option[String] = None,
        imageUrl: scala.Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[User]] = {
        val payload = play.api.libs.json.Json.obj(
          "email" -> play.api.libs.json.Json.toJson(email),
          "name" -> play.api.libs.json.Json.toJson(name),
          "image_url" -> play.api.libs.json.Json.toJson(imageUrl)
        )
        
        PUT(s"/users/${({x: java.util.UUID =>
          val s = x.toString
          java.net.URLEncoder.encode(s, "UTF-8")
        })(guid)}", payload).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[User], 200)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.Seq[Error]], 409)
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
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.Seq[Version]]] = {
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
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.Seq[Version]], 200)
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
