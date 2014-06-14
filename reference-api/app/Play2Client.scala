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

  object Big {
    case class Patch(
      f1: scala.Option[String] = None,
      f2: scala.Option[String] = None,
      f3: scala.Option[String] = None,
      f4: scala.Option[String] = None,
      f5: scala.Option[String] = None,
      f6: scala.Option[String] = None,
      f7: scala.Option[String] = None,
      f8: scala.Option[String] = None,
      f9: scala.Option[String] = None,
      f10: scala.Option[String] = None,
      f11: scala.Option[String] = None,
      f12: scala.Option[String] = None,
      f13: scala.Option[String] = None,
      f14: scala.Option[String] = None,
      f15: scala.Option[String] = None,
      f16: scala.Option[String] = None,
      f17: scala.Option[String] = None,
      f18: scala.Option[String] = None,
      f19: scala.Option[String] = None,
      f20: scala.Option[String] = None,
      f21: scala.Option[String] = None
    ) {
    
      def f1(value: String): Patch = copy(f1 = Option(value))
      
      def f2(value: String): Patch = copy(f2 = Option(value))
      
      def f3(value: String): Patch = copy(f3 = Option(value))
      
      def f4(value: String): Patch = copy(f4 = Option(value))
      
      def f5(value: String): Patch = copy(f5 = Option(value))
      
      def f6(value: String): Patch = copy(f6 = Option(value))
      
      def f7(value: String): Patch = copy(f7 = Option(value))
      
      def f8(value: String): Patch = copy(f8 = Option(value))
      
      def f9(value: String): Patch = copy(f9 = Option(value))
      
      def f10(value: String): Patch = copy(f10 = Option(value))
      
      def f11(value: String): Patch = copy(f11 = Option(value))
      
      def f12(value: String): Patch = copy(f12 = Option(value))
      
      def f13(value: String): Patch = copy(f13 = Option(value))
      
      def f14(value: String): Patch = copy(f14 = Option(value))
      
      def f15(value: String): Patch = copy(f15 = Option(value))
      
      def f16(value: String): Patch = copy(f16 = Option(value))
      
      def f17(value: String): Patch = copy(f17 = Option(value))
      
      def f18(value: String): Patch = copy(f18 = Option(value))
      
      def f19(value: String): Patch = copy(f19 = Option(value))
      
      def f20(value: String): Patch = copy(f20 = Option(value))
      
      def f21(value: String): Patch = copy(f21 = Option(value))
    
      def apply(x: Big): Big = x.copy(
        f1 = f1.getOrElse(x.f1),
        f2 = f2.getOrElse(x.f2),
        f3 = f3.getOrElse(x.f3),
        f4 = f4.getOrElse(x.f4),
        f5 = f5.getOrElse(x.f5),
        f6 = f6.getOrElse(x.f6),
        f7 = f7.getOrElse(x.f7),
        f8 = f8.getOrElse(x.f8),
        f9 = f9.getOrElse(x.f9),
        f10 = f10.getOrElse(x.f10),
        f11 = f11.getOrElse(x.f11),
        f12 = f12.getOrElse(x.f12),
        f13 = f13.getOrElse(x.f13),
        f14 = f14.getOrElse(x.f14),
        f15 = f15.getOrElse(x.f15),
        f16 = f16.getOrElse(x.f16),
        f17 = f17.getOrElse(x.f17),
        f18 = f18.getOrElse(x.f18),
        f19 = f19.getOrElse(x.f19),
        f20 = f20.getOrElse(x.f20),
        f21 = f21.getOrElse(x.f21)
      )
    }
  }

  case class Error(
    code: String,
    message: String
  )

  object Error {
    case class Patch(
      code: scala.Option[String] = None,
      message: scala.Option[String] = None
    ) {
    
      def code(value: String): Patch = copy(code = Option(value))
      
      def message(value: String): Patch = copy(message = Option(value))
    
      def apply(x: Error): Error = x.copy(
        code = code.getOrElse(x.code),
        message = message.getOrElse(x.message)
      )
    }
  }

  case class Member(
    guid: java.util.UUID,
    organization: Organization,
    user: User,
    role: String
  )

  object Member {
    case class Patch(
      guid: scala.Option[java.util.UUID] = None,
      organization: scala.Option[Organization] = None,
      user: scala.Option[User] = None,
      role: scala.Option[String] = None
    ) {
    
      def guid(value: java.util.UUID): Patch = copy(guid = Option(value))
      
      def organization(value: Organization): Patch = copy(organization = Option(value))
      
      def user(value: User): Patch = copy(user = Option(value))
      
      def role(value: String): Patch = copy(role = Option(value))
    
      def apply(x: Member): Member = x.copy(
        guid = guid.getOrElse(x.guid),
        organization = organization.getOrElse(x.organization),
        user = user.getOrElse(x.user),
        role = role.getOrElse(x.role)
      )
    }
  }

  case class MemberForm(
    organization: java.util.UUID,
    user: java.util.UUID,
    role: String
  )

  object MemberForm {
    case class Patch(
      organization: scala.Option[java.util.UUID] = None,
      user: scala.Option[java.util.UUID] = None,
      role: scala.Option[String] = None
    ) {
    
      def organization(value: java.util.UUID): Patch = copy(organization = Option(value))
      
      def user(value: java.util.UUID): Patch = copy(user = Option(value))
      
      def role(value: String): Patch = copy(role = Option(value))
    
      def apply(x: MemberForm): MemberForm = x.copy(
        organization = organization.getOrElse(x.organization),
        user = user.getOrElse(x.user),
        role = role.getOrElse(x.role)
      )
    }
  }

  case class Organization(
    guid: java.util.UUID,
    name: String
  )

  object Organization {
    case class Patch(
      guid: scala.Option[java.util.UUID] = None,
      name: scala.Option[String] = None
    ) {
    
      def guid(value: java.util.UUID): Patch = copy(guid = Option(value))
      
      def name(value: String): Patch = copy(name = Option(value))
    
      def apply(x: Organization): Organization = x.copy(
        guid = guid.getOrElse(x.guid),
        name = name.getOrElse(x.name)
      )
    }
  }

  case class User(
    guid: java.util.UUID,
    email: String,
    active: Boolean
  )

  object User {
    case class Patch(
      guid: scala.Option[java.util.UUID] = None,
      email: scala.Option[String] = None,
      active: scala.Option[Boolean] = None
    ) {
    
      def guid(value: java.util.UUID): Patch = copy(guid = Option(value))
      
      def email(value: String): Patch = copy(email = Option(value))
      
      def active(value: Boolean): Patch = copy(active = Option(value))
    
      def apply(x: User): User = x.copy(
        guid = guid.getOrElse(x.guid),
        email = email.getOrElse(x.email),
        active = active.getOrElse(x.active)
      )
    }
  }

  case class UserForm(
    email: String
  )

  object UserForm {
    case class Patch(
      email: scala.Option[String] = None
    ) {
    
      def email(value: String): Patch = copy(email = Option(value))
    
      def apply(x: UserForm): UserForm = x.copy(
        email = email.getOrElse(x.email)
      )
    }
  }

  case class UserList(
    users: scala.collection.Seq[User]
  )

  object UserList {
    case class Patch(
      users: scala.Option[scala.collection.Seq[User]] = None
    ) {
    
      def users(value: scala.collection.Seq[User]): Patch = copy(users = Option(value))
    
      def apply(x: UserList): UserList = x.copy(
        users = users.getOrElse(x.users)
      )
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
    
    implicit def readsMemberForm: play.api.libs.json.Reads[MemberForm] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "organization").read[java.util.UUID] and
         (__ \ "user").read[java.util.UUID] and
         (__ \ "role").read[String])(MemberForm.apply _)
      }
    
    implicit def writesMemberForm: play.api.libs.json.Writes[MemberForm] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "organization").write[java.util.UUID] and
         (__ \ "user").write[java.util.UUID] and
         (__ \ "role").write[String])(unlift(MemberForm.unapply))
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
    
    implicit def readsUserForm: play.api.libs.json.Reads[UserForm] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        (__ \ "email").read[String].map { x =>
          new UserForm(email = x)
        }
      }
    
    implicit def writesUserForm: play.api.libs.json.Writes[UserForm] =
      new play.api.libs.json.Writes[UserForm] {
        def writes(x: UserForm) = play.api.libs.json.Json.obj(
          "email" -> play.api.libs.json.Json.toJson(x.email)
        )
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
        _body: MemberForm
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Member]] = {
        val payload = play.api.libs.json.Json.toJson(_body)
        val queryBuilder = List.newBuilder[(String, String)]
        
        val query = queryBuilder.result
        processResponse(logRequest("POST", requestHolder(s"/members"))
          .withQueryString(query:_*).post(payload)).map {
          case r if r.status == 201 => new ResponseImpl(r.json.as[Member], 201)
          case r if r.status == 409 => throw new FailedResponse(r.json.as[scala.collection.Seq[Error]], 409)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      def get(
        guid: scala.Option[java.util.UUID] = None,
        organization: scala.Option[java.util.UUID] = None,
        user: scala.Option[java.util.UUID] = None,
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
        queryBuilder ++= organization.map { x =>
          "organization" -> (
            { x: java.util.UUID =>
              x.toString
            }
          )(x)
        }
        queryBuilder ++= user.map { x =>
          "user" -> (
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
        val query = queryBuilder.result
        processResponse(logRequest("GET", requestHolder(s"/members")
          .withQueryString(query:_*)).get()).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.Seq[Member]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      def getByOrganization(
        organization: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[scala.collection.Seq[Member]]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        
        val query = queryBuilder.result
        processResponse(logRequest("GET", requestHolder(s"/members/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(organization)}")
          .withQueryString(query:_*)).get()).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.Seq[Member]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
    
    object Organizations {
      def post(
        _body: Organization
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Organization]] = {
        val payload = play.api.libs.json.Json.toJson(_body)
        val queryBuilder = List.newBuilder[(String, String)]
        
        val query = queryBuilder.result
        processResponse(logRequest("POST", requestHolder(s"/organizations"))
          .withQueryString(query:_*).post(payload)).map {
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
        val query = queryBuilder.result
        processResponse(logRequest("GET", requestHolder(s"/organizations")
          .withQueryString(query:_*)).get()).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.Seq[Organization]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      def getByGuid(
        guid: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Organization]] = {
        val queryBuilder = List.newBuilder[(String, String)]
        
        val query = queryBuilder.result
        processResponse(logRequest("GET", requestHolder(s"/organizations/${({x: String =>
          val s = x
          java.net.URLEncoder.encode(s, "UTF-8")
        })(guid)}")
          .withQueryString(query:_*)).get()).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[Organization], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
    
    object Users {
      def post(
        active: scala.Option[Boolean] = None,
        _body: UserForm
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[User]] = {
        val payload = play.api.libs.json.Json.toJson(_body)
        val queryBuilder = List.newBuilder[(String, String)]
        queryBuilder ++= active.map { x =>
          "active" -> (
            { x: Boolean =>
              x.toString
            }
          )(x)
        }
        val query = queryBuilder.result
        processResponse(logRequest("POST", requestHolder(s"/users"))
          .withQueryString(query:_*).post(payload)).map {
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
        val query = queryBuilder.result
        processResponse(logRequest("GET", requestHolder(s"/users")
          .withQueryString(query:_*)).get()).map {
          case r if r.status == 200 => new ResponseImpl(r.json.as[scala.collection.Seq[User]], 200)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      def postNoop(
        _body: scala.Unit = ()
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Unit]] = {
        val payload = ""
        val queryBuilder = List.newBuilder[(String, String)]
        
        val query = queryBuilder.result
        processResponse(logRequest("POST", requestHolder(s"/users/noop"))
          .withQueryString(query:_*).post(payload)).map {
          case r if r.status == 204 => new ResponseImpl((), 204)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
      
      def postProfileByGuid(
        guid: java.util.UUID,
        _body: java.io.File
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response[Unit]] = {
        val payload = _body
        val queryBuilder = List.newBuilder[(String, String)]
        
        val query = queryBuilder.result
        processResponse(logRequest("POST", requestHolder(s"/users/${({x: java.util.UUID =>
          val s = x.toString
          java.net.URLEncoder.encode(s, "UTF-8")
        })(guid)}/profile"))
          .withQueryString(query:_*).post(payload)).map {
          case r if r.status == 204 => new ResponseImpl((), 204)
          case r => throw new FailedResponse(r.body, r.status)
        }
      }
    }
  }
}
