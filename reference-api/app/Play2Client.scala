package referenceapi.models {
  case class Big(
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
  )
  case class Error(
    code: String,
    message: String
  )
  case class Member(
    guid: java.util.UUID,
    organization: Organization,
    user: User,
    role: String
  )
  case class Organization(
    guid: java.util.UUID,
    name: String
  )
  case class User(
    guid: java.util.UUID,
    email: String,
    active: Boolean
  )
  case class UserList(
    users: scala.collection.Seq[User]
  )
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

    implicit def readsBig: play.api.libs.json.Reads[Big] =
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
         (__ \ "f21").read[String])(Big.apply _)
      }
    
    implicit def writesBig: play.api.libs.json.Writes[Big] =
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
    
    implicit def readsMember: play.api.libs.json.Reads[Member] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "organization").read[Organization] and
         (__ \ "user").read[User] and
         (__ \ "role").read[String])(Member.apply _)
      }
    
    implicit def writesMember: play.api.libs.json.Writes[Member] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "organization").write[Organization] and
         (__ \ "user").write[User] and
         (__ \ "role").write[String])(unlift(Member.unapply))
      }
    
    implicit def readsOrganization: play.api.libs.json.Reads[Organization] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "name").read[String])(Organization.apply _)
      }
    
    implicit def writesOrganization: play.api.libs.json.Writes[Organization] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "name").write[String])(unlift(Organization.unapply))
      }
    
    implicit def readsUser: play.api.libs.json.Reads[User] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "email").read[String] and
         (__ \ "active").read[Boolean])(User.apply _)
      }
    
    implicit def writesUser: play.api.libs.json.Writes[User] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "email").write[String] and
         (__ \ "active").write[Boolean])(unlift(User.unapply))
      }
    
    implicit def readsUserList: play.api.libs.json.Reads[UserList] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        (__ \ "users").readNullable[scala.collection.Seq[User]].map { x =>
        x.getOrElse(Nil)
      }.map { x =>
          new UserList(users = x)
        }
      }
    
    implicit def writesUserList: play.api.libs.json.Writes[UserList] =
      new play.api.libs.json.Writes[UserList] {
        def writes(x: UserList) = play.api.libs.json.Json.obj(
          "users" -> play.api.libs.json.Json.toJson(x.users)
        )
      }
  }
}

package referenceapi {
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
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.Seq[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      def get(
        guid: scala.Option[java.util.UUID] = None,
        organizationGuid: scala.Option[java.util.UUID] = None,
        userGuid: scala.Option[java.util.UUID] = None,
        role: scala.Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.Seq[Member]]] = {
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
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.Seq[Member]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      def getByOrganization(
        organization: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.Seq[Member]]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        
        
        GET(s"/members/${({x: java.util.UUID =>
          val s = x.toString
          java.net.URLEncoder.encode(s, "UTF-8")
        })(organization)}", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.Seq[Member]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
    
    object Organizations {
      def post(
        organization: Organization
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Organization]] = {
        val payload = play.api.libs.json.Json.obj(
          "organization" -> play.api.libs.json.Json.toJson(organization)
        )
        
        POST(s"/organizations", payload).map {
          case r if r.status == 201 => new ResponseImpl(r.json.as[Organization], 201)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.Seq[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      def get(
        guid: scala.Option[java.util.UUID] = None,
        name: scala.Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.Seq[Organization]]] = {
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
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.Seq[Organization]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      def getByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Organization]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        
        
        GET(s"/organizations/${({x: java.util.UUID =>
          val s = x.toString
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
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.Seq[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      def get(
        guid: scala.Option[java.util.UUID] = None,
        email: scala.Option[String] = None,
        active: scala.Option[Boolean] = None
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
        queryBuilder ++= active.map { x =>
          "active" -> (
            { x: Boolean =>
              x.toString
            }
          )(x)
        }
        
        GET(s"/users", queryBuilder.result).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.Seq[User]], 200)
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
