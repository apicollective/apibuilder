package referenceapi.models {
  /**
   * A model with a lot of fields.
   */
  trait Big {
    def f1: String
    
    def f2: String
    
    def f3: String
    
    def f4: String
    
    def f5: String
    
    def f6: String
    
    def f7: String
    
    def f8: String
    
    def f9: String
    
    def f10: String
    
    def f11: String
    
    def f12: String
    
    def f13: String
    
    def f14: String
    
    def f15: String
    
    def f16: String
    
    def f17: String
    
    def f18: String
    
    def f19: String
    
    def f20: String
    
    def f21: String
  }

  case class BigImpl(
    f1: String,
    f2: String,
    f3: String,
    f4: String,
    f5: String,
    f6: String,
    f7: String,
    f8: String,
    f9: String,
    f10: String,
    f11: String,
    f12: String,
    f13: String,
    f14: String,
    f15: String,
    f16: String,
    f17: String,
    f18: String,
    f19: String,
    f20: String,
    f21: String
  ) extends Big

  object Big {
    def apply(f1: String, f2: String, f3: String, f4: String, f5: String, f6: String, f7: String, f8: String, f9: String, f10: String, f11: String, f12: String, f13: String, f14: String, f15: String, f16: String, f17: String, f18: String, f19: String, f20: String, f21: String): BigImpl = {
      new BigImpl(f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13,f14,f15,f16,f17,f18,f19,f20,f21)
    }
  
    def unapply(x: Big) = {
      Some(x.f1, x.f2, x.f3, x.f4, x.f5, x.f6, x.f7, x.f8, x.f9, x.f10, x.f11, x.f12, x.f13, x.f14, x.f15, x.f16, x.f17, x.f18, x.f19, x.f20, x.f21)
    }
  
    import scala.language.implicitConversions
    implicit def toImpl(x: Big): BigImpl = x match {
      case impl: BigImpl => impl
      case _ => new BigImpl(x.f1,x.f2,x.f3,x.f4,x.f5,x.f6,x.f7,x.f8,x.f9,x.f10,x.f11,x.f12,x.f13,x.f14,x.f15,x.f16,x.f17,x.f18,x.f19,x.f20,x.f21)
    }
  }
  /**
   * Models an API error.
   */
  trait Error {
    def code: String
    
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
  trait Organization {
    def guid: java.util.UUID
    
    def name: String
  }

  case class OrganizationImpl(
    guid: java.util.UUID,
    name: String
  ) extends Organization

  object Organization {
    def apply(guid: java.util.UUID, name: String): OrganizationImpl = {
      new OrganizationImpl(guid,name)
    }
  
    def unapply(x: Organization) = {
      Some(x.guid, x.name)
    }
  
    import scala.language.implicitConversions
    implicit def toImpl(x: Organization): OrganizationImpl = x match {
      case impl: OrganizationImpl => impl
      case _ => new OrganizationImpl(x.guid,x.name)
    }
  }
  trait User {
    def guid: java.util.UUID
    
    def email: String
    
    def active: Boolean
  }

  case class UserImpl(
    guid: java.util.UUID,
    email: String,
    active: Boolean
  ) extends User

  object User {
    def apply(guid: java.util.UUID, email: String, active: Boolean): UserImpl = {
      new UserImpl(guid,email,active)
    }
  
    def unapply(x: User) = {
      Some(x.guid, x.email, x.active)
    }
  
    import scala.language.implicitConversions
    implicit def toImpl(x: User): UserImpl = x match {
      case impl: UserImpl => impl
      case _ => new UserImpl(x.guid,x.email,x.active)
    }
  }
  trait Member {
    def guid: java.util.UUID
    
    def organization: Organization
    
    def user: User
    
    def role: String
  }

  case class MemberImpl(
    guid: java.util.UUID,
    organization: Organization,
    user: User,
    role: String
  ) extends Member

  object Member {
    def apply(guid: java.util.UUID, organization: Organization, user: User, role: String): MemberImpl = {
      new MemberImpl(guid,organization,user,role)
    }
  
    def unapply(x: Member) = {
      Some(x.guid, x.organization, x.user, x.role)
    }
  
    import scala.language.implicitConversions
    implicit def toImpl(x: Member): MemberImpl = x match {
      case impl: MemberImpl => impl
      case _ => new MemberImpl(x.guid,x.organization,x.user,x.role)
    }
  }
}

package referenceapi.models {
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

    implicit val readsBig: play.api.libs.json.Reads[Big] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "f1").read[String] and
         (__ \ "f2").read[String] and
         (__ \ "f3").read[String] and
         (__ \ "f4").read[String] and
         (__ \ "f5").read[String] and
         (__ \ "f6").read[String] and
         (__ \ "f7").read[String] and
         (__ \ "f8").read[String] and
         (__ \ "f9").read[String] and
         (__ \ "f10").read[String] and
         (__ \ "f11").read[String] and
         (__ \ "f12").read[String] and
         (__ \ "f13").read[String] and
         (__ \ "f14").read[String] and
         (__ \ "f15").read[String] and
         (__ \ "f16").read[String] and
         (__ \ "f17").read[String] and
         (__ \ "f18").read[String] and
         (__ \ "f19").read[String] and
         (__ \ "f20").read[String] and
         (__ \ "f21").read[String])(BigImpl.apply _)
      }
    
    implicit val writesBig: play.api.libs.json.Writes[Big] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "f1").write[String] and
         (__ \ "f2").write[String] and
         (__ \ "f3").write[String] and
         (__ \ "f4").write[String] and
         (__ \ "f5").write[String] and
         (__ \ "f6").write[String] and
         (__ \ "f7").write[String] and
         (__ \ "f8").write[String] and
         (__ \ "f9").write[String] and
         (__ \ "f10").write[String] and
         (__ \ "f11").write[String] and
         (__ \ "f12").write[String] and
         (__ \ "f13").write[String] and
         (__ \ "f14").write[String] and
         (__ \ "f15").write[String] and
         (__ \ "f16").write[String] and
         (__ \ "f17").write[String] and
         (__ \ "f18").write[String] and
         (__ \ "f19").write[String] and
         (__ \ "f20").write[String] and
         (__ \ "f21").write[String])(unlift(Big.unapply))
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
    
    implicit val readsOrganization: play.api.libs.json.Reads[Organization] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "name").read[String])(OrganizationImpl.apply _)
      }
    
    implicit val writesOrganization: play.api.libs.json.Writes[Organization] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "name").write[String])(unlift(Organization.unapply))
      }
    
    implicit val readsUser: play.api.libs.json.Reads[User] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "email").read[String] and
         (__ \ "active").read[Boolean])(UserImpl.apply _)
      }
    
    implicit val writesUser: play.api.libs.json.Writes[User] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "email").write[String] and
         (__ \ "active").write[Boolean])(unlift(User.unapply))
      }
    
    implicit val readsMember: play.api.libs.json.Reads[Member] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "organization").read[Organization] and
         (__ \ "user").read[User] and
         (__ \ "role").read[String])(MemberImpl.apply _)
      }
    
    implicit val writesMember: play.api.libs.json.Writes[Member] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "organization").write[Organization] and
         (__ \ "user").write[User] and
         (__ \ "role").write[String])(unlift(Member.unapply))
      }
  }
}

package referenceapi {
  class Client(apiUrl: String, apiToken: Option[String] = None) {
    import referenceapi.models._
    import referenceapi.models.json._

    private val logger = play.api.Logger("referenceapi.client")

    logger.info(s"Initializing referenceapi.client for url $apiUrl")

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

    object Members {
      def post(
        guid: java.util.UUID,
        organization: java.util.UUID,
        user: java.util.UUID,
        role: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Member]] = {
        val payload = play.api.libs.json.Json.obj(
          "guid" -> play.api.libs.json.Json.toJson(guid),
          "organization" -> play.api.libs.json.Json.toJson(organization),
          "user" -> play.api.libs.json.Json.toJson(user),
          "role" -> play.api.libs.json.Json.toJson(role)
        )
        
        POST(s"/members", payload).map {
          case r if r.status == 201 => new ResponseImpl(r.json.as[Member], 201)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.immutable.Seq[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      def get(
        guid: scala.Option[java.util.UUID] = None,
        organizationGuid: scala.Option[java.util.UUID] = None,
        userGuid: scala.Option[java.util.UUID] = None,
        role: scala.Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.immutable.Seq[Member]]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        queryBuilder ++= guid.map { x =>
          "guid" -> (
            { x: java.util.UUID =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= organizationGuid.map { x =>
          "organization_guid" -> (
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
        
        GET(s"/members", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.immutable.Seq[Member]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      def getByOrganization(
        organization: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.immutable.Seq[Member]]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        
        
        GET(s"/members/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(organization)}", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.immutable.Seq[Member]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
    
    object Organizations {
      def post(
        guid: java.util.UUID,
        name: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Organization]] = {
        val payload = play.api.libs.json.Json.obj(
          "guid" -> play.api.libs.json.Json.toJson(guid),
          "name" -> play.api.libs.json.Json.toJson(name)
        )
        
        POST(s"/organizations", payload).map {
          case r if r.status == 201 => new ResponseImpl(r.json.as[Organization], 201)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.immutable.Seq[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      def get(
        guid: scala.Option[java.util.UUID] = None,
        name: scala.Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.immutable.Seq[Organization]]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        queryBuilder ++= guid.map { x =>
          "guid" -> (
            { x: java.util.UUID =>
              x.toString
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
        
        GET(s"/organizations", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.immutable.Seq[Organization]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      def getByGuid(
        guid: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Organization]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        
        
        GET(s"/organizations/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(guid)}", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[Organization], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
    
    object Users {
      def post(
        guid: java.util.UUID,
        email: String,
        active: Boolean
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[User]] = {
        val payload = play.api.libs.json.Json.obj(
          "guid" -> play.api.libs.json.Json.toJson(guid),
          "email" -> play.api.libs.json.Json.toJson(email),
          "active" -> play.api.libs.json.Json.toJson(active)
        )
        
        POST(s"/users", payload).map {
          case r if r.status == 201 => new ResponseImpl(r.json.as[User], 201)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.immutable.Seq[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      def get(
        guid: scala.Option[java.util.UUID] = None,
        email: scala.Option[String] = None,
        active: scala.Option[Boolean] = None
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
        queryBuilder ++= active.map { x =>
          "active" -> (
            { x: Boolean =>
              x.toString
            }
          )(x)
        }
        
        GET(s"/users", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.immutable.Seq[User]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      def postNoop(
      
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Unit]] = {
        val payload = play.api.libs.json.Json.obj(
          
        )
        
        POST(s"/users/noop", payload).map {
          case r if r.status == 200 => new ResponseImpl((), 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
  }
}
