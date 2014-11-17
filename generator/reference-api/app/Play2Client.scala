package referenceapi.models {
  /**
   * A model with a lot of fields.
   */
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

  case class Echo(
    value: String
  )

  /**
   * Models an API error.
   */
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
    active: Boolean,
    ageGroup: AgeGroup
  )

  case class UserList(
    users: Seq[User]
  )

  sealed trait AgeGroup

  object AgeGroup {

    /**
     * under 18
     */
    case object Youth extends AgeGroup { override def toString = "Youth" }
    /**
     * 18 and over
     */
    case object Adult extends AgeGroup { override def toString = "Adult" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends AgeGroup

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(Youth, Adult)

    private[this]
    val byName = all.map(x => x.toString -> x).toMap

    def apply(value: String): AgeGroup = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): scala.Option[AgeGroup] = byName.get(value)

  }
}

package referenceapi.models {
  package object json {
    import play.api.libs.json.__
    import play.api.libs.json.JsString
    import play.api.libs.json.Writes
    import play.api.libs.functional.syntax._

    private[referenceapi] implicit val jsonReadsUUID = __.read[String].map(java.util.UUID.fromString)

    private[referenceapi] implicit val jsonWritesUUID = new Writes[java.util.UUID] {
      def writes(x: java.util.UUID) = JsString(x.toString)
    }

    private[referenceapi] implicit val jsonReadsJodaDateTime = __.read[String].map { str =>
      import org.joda.time.format.ISODateTimeFormat.dateTimeParser
      dateTimeParser.parseDateTime(str)
    }

    private[referenceapi] implicit val jsonWritesJodaDateTime = new Writes[org.joda.time.DateTime] {
      def writes(x: org.joda.time.DateTime) = {
        import org.joda.time.format.ISODateTimeFormat.dateTime
        val str = dateTime.print(x)
        JsString(str)
      }
    }

    implicit val jsonReadsReferenceApiEnum_AgeGroup = __.read[String].map(AgeGroup.apply)
    implicit val jsonWritesReferenceApiEnum_AgeGroup = new Writes[AgeGroup] {
      def writes(x: AgeGroup) = JsString(x.toString)
    }
    implicit def jsonReadsReferenceApiBig: play.api.libs.json.Reads[Big] = {
      (
        (__ \ "f1").read[String] and
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
        (__ \ "f21").read[String]
      )(Big.apply _)
    }

    implicit def jsonWritesReferenceApiBig: play.api.libs.json.Writes[Big] = {
      (
        (__ \ "f1").write[String] and
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
        (__ \ "f21").write[String]
      )(unlift(Big.unapply _))
    }

    implicit def jsonReadsReferenceApiEcho: play.api.libs.json.Reads[Echo] = {
      (__ \ "value").read[String].map { x => new Echo(value = x) }
    }

    implicit def jsonWritesReferenceApiEcho: play.api.libs.json.Writes[Echo] = new play.api.libs.json.Writes[Echo] {
      def writes(x: Echo) = play.api.libs.json.Json.obj(
        "value" -> play.api.libs.json.Json.toJson(x.value)
      )
    }

    implicit def jsonReadsReferenceApiError: play.api.libs.json.Reads[Error] = {
      (
        (__ \ "code").read[String] and
        (__ \ "message").read[String]
      )(Error.apply _)
    }

    implicit def jsonWritesReferenceApiError: play.api.libs.json.Writes[Error] = {
      (
        (__ \ "code").write[String] and
        (__ \ "message").write[String]
      )(unlift(Error.unapply _))
    }

    implicit def jsonReadsReferenceApiMember: play.api.libs.json.Reads[Member] = {
      (
        (__ \ "guid").read[java.util.UUID] and
        (__ \ "organization").read[Organization] and
        (__ \ "user").read[User] and
        (__ \ "role").read[String]
      )(Member.apply _)
    }

    implicit def jsonWritesReferenceApiMember: play.api.libs.json.Writes[Member] = {
      (
        (__ \ "guid").write[java.util.UUID] and
        (__ \ "organization").write[Organization] and
        (__ \ "user").write[User] and
        (__ \ "role").write[String]
      )(unlift(Member.unapply _))
    }

    implicit def jsonReadsReferenceApiOrganization: play.api.libs.json.Reads[Organization] = {
      (
        (__ \ "guid").read[java.util.UUID] and
        (__ \ "name").read[String]
      )(Organization.apply _)
    }

    implicit def jsonWritesReferenceApiOrganization: play.api.libs.json.Writes[Organization] = {
      (
        (__ \ "guid").write[java.util.UUID] and
        (__ \ "name").write[String]
      )(unlift(Organization.unapply _))
    }

    implicit def jsonReadsReferenceApiUser: play.api.libs.json.Reads[User] = {
      (
        (__ \ "guid").read[java.util.UUID] and
        (__ \ "email").read[String] and
        (__ \ "active").read[Boolean] and
        (__ \ "age_group").read[AgeGroup]
      )(User.apply _)
    }

    implicit def jsonWritesReferenceApiUser: play.api.libs.json.Writes[User] = {
      (
        (__ \ "guid").write[java.util.UUID] and
        (__ \ "email").write[String] and
        (__ \ "active").write[Boolean] and
        (__ \ "age_group").write[AgeGroup]
      )(unlift(User.unapply _))
    }

    implicit def jsonReadsReferenceApiUserList: play.api.libs.json.Reads[UserList] = {
      (__ \ "users").readNullable[Seq[User]].map(_.getOrElse(Nil)).map { x => new UserList(users = x) }
    }

    implicit def jsonWritesReferenceApiUserList: play.api.libs.json.Writes[UserList] = new play.api.libs.json.Writes[UserList] {
      def writes(x: UserList) = play.api.libs.json.Json.obj(
        "users" -> play.api.libs.json.Json.toJson(x.users)
      )
    }
  }
}

package referenceapi {
  object helpers {
    import org.joda.time.DateTime
    import org.joda.time.format.ISODateTimeFormat
    import play.api.mvc.QueryStringBindable

    import scala.util.{ Failure, Success, Try }

    private[helpers] val dateTimeISOParser = ISODateTimeFormat.dateTimeParser()
    private[helpers] val dateTimeISOFormatter = ISODateTimeFormat.dateTime()

    private[helpers] def parseDateTimeISO(s: String): Either[String, DateTime] = {
      Try(dateTimeISOParser.parseDateTime(s)) match {
        case Success(dt) => Right(dt)
        case Failure(f) => Left("Could not parse DateTime: " + f.getMessage)
      }
    }

    implicit object DateTimeISOQueryStringBinder extends QueryStringBindable[DateTime] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, DateTime]] = {
        for {
          values <- params.get(key)
          s <- values.headOption
        } yield parseDateTimeISO(s)
      }

      override def unbind(key: String, time: DateTime): String = key + "=" + dateTimeISOFormatter.print(time)
    }
  }

  class Client(apiUrl: String, apiToken: scala.Option[String] = None) {
    import referenceapi.models._
    import referenceapi.models.json._

    private val logger = play.api.Logger("referenceapi.client")

    logger.info(s"Initializing referenceapi.client for url $apiUrl")

    def echos: Echos = Echos

    def members: Members = Members

    def organizations: Organizations = Organizations

    def users: Users = Users

    trait Echos {
      def get(
        foo: scala.Option[String] = None,
        optionalMessages: Seq[String] = Nil,
        requiredMessages: Seq[String]
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]]
    }

    object Echos extends Echos {
      override def get(
        foo: scala.Option[String] = None,
        optionalMessages: Seq[String] = Nil,
        requiredMessages: Seq[String]
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]] = {
        val query = Seq(
          foo.map("foo" -> _),
          optionalMessages.map("optional_messages" -> _),
          Some("required_messages" -> requiredMessages)
        ).flatten

        GET(s"/echos", query).map {
          case r if r.status == 204 => Some(Unit)
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
    }

    trait Members {
      def post(
        guid: java.util.UUID,
        organization: java.util.UUID,
        user: java.util.UUID,
        role: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[referenceapi.models.Member]

      def get(
        guid: scala.Option[java.util.UUID] = None,
        organizationGuid: scala.Option[java.util.UUID] = None,
        userGuid: scala.Option[java.util.UUID] = None,
        role: scala.Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[referenceapi.models.Member]]

      def getByOrganization(
        organization: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[referenceapi.models.Member]]
    }

    object Members extends Members {
      override def post(
        guid: java.util.UUID,
        organization: java.util.UUID,
        user: java.util.UUID,
        role: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[referenceapi.models.Member] = {
        val payload = play.api.libs.json.Json.obj(
          "guid" -> play.api.libs.json.Json.toJson(guid),
          "organization" -> play.api.libs.json.Json.toJson(organization),
          "user" -> play.api.libs.json.Json.toJson(user),
          "role" -> play.api.libs.json.Json.toJson(role)
        )

        POST(s"/members", payload).map {
          case r if r.status == 201 => r.json.as[referenceapi.models.Member]
          case r if r.status == 409 => throw new referenceapi.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def get(
        guid: scala.Option[java.util.UUID] = None,
        organizationGuid: scala.Option[java.util.UUID] = None,
        userGuid: scala.Option[java.util.UUID] = None,
        role: scala.Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[referenceapi.models.Member]] = {
        val query = Seq(
          guid.map("guid" -> _.toString),
          organizationGuid.map("organization_guid" -> _.toString),
          userGuid.map("user_guid" -> _.toString),
          role.map("role" -> _)
        ).flatten

        GET(s"/members", query).map {
          case r if r.status == 200 => r.json.as[Seq[referenceapi.models.Member]]
          case r => throw new FailedRequest(r)
        }
      }

      override def getByOrganization(
        organization: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[referenceapi.models.Member]] = {
        GET(s"/members/${organization}").map {
          case r if r.status == 200 => r.json.as[Seq[referenceapi.models.Member]]
          case r => throw new FailedRequest(r)
        }
      }
    }

    trait Organizations {
      def post(
        organization: Organization
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[referenceapi.models.Organization]

      def get(
        guid: scala.Option[java.util.UUID] = None,
        name: scala.Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[referenceapi.models.Organization]]

      def getByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[referenceapi.models.Organization]]
    }

    object Organizations extends Organizations {
      override def post(
        organization: Organization
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[referenceapi.models.Organization] = {
        val payload = play.api.libs.json.Json.obj(
          "organization" -> play.api.libs.json.Json.toJson(organization)
        )

        POST(s"/organizations", payload).map {
          case r if r.status == 201 => r.json.as[referenceapi.models.Organization]
          case r if r.status == 409 => throw new referenceapi.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def get(
        guid: scala.Option[java.util.UUID] = None,
        name: scala.Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[referenceapi.models.Organization]] = {
        val query = Seq(
          guid.map("guid" -> _.toString),
          name.map("name" -> _)
        ).flatten

        GET(s"/organizations", query).map {
          case r if r.status == 200 => r.json.as[Seq[referenceapi.models.Organization]]
          case r => throw new FailedRequest(r)
        }
      }

      override def getByGuid(
        guid: java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[referenceapi.models.Organization]] = {
        GET(s"/organizations/${guid}").map {
          case r if r.status == 200 => Some(r.json.as[referenceapi.models.Organization])
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
    }

    trait Users {
      def post(
        guid: java.util.UUID,
        email: String,
        active: Boolean
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[referenceapi.models.User]

      def get(
        guid: scala.Option[java.util.UUID] = None,
        email: scala.Option[String] = None,
        active: scala.Option[Boolean] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[referenceapi.models.User]]

      def postNoop()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Unit]
    }

    object Users extends Users {
      override def post(
        guid: java.util.UUID,
        email: String,
        active: Boolean
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[referenceapi.models.User] = {
        val payload = play.api.libs.json.Json.obj(
          "guid" -> play.api.libs.json.Json.toJson(guid),
          "email" -> play.api.libs.json.Json.toJson(email),
          "active" -> play.api.libs.json.Json.toJson(active)
        )

        POST(s"/users", payload).map {
          case r if r.status == 201 => r.json.as[referenceapi.models.User]
          case r if r.status == 409 => throw new referenceapi.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def get(
        guid: scala.Option[java.util.UUID] = None,
        email: scala.Option[String] = None,
        active: scala.Option[Boolean] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[referenceapi.models.User]] = {
        val query = Seq(
          guid.map("guid" -> _.toString),
          email.map("email" -> _),
          active.map("active" -> _.toString)
        ).flatten

        GET(s"/users", query).map {
          case r if r.status == 200 => r.json.as[Seq[referenceapi.models.User]]
          case r => throw new FailedRequest(r)
        }
      }

      override def postNoop()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Unit] = {
        POST(s"/users/noop").map {
          case r if r.status == 200 => Unit
          case r => throw new FailedRequest(r)
        }
      }
    }

    private val UserAgent = "apidoc gilt 0.0.1-reference"

    def _requestHolder(path: String): play.api.libs.ws.WSRequestHolder = {
      import play.api.Play.current

      val holder = play.api.libs.ws.WS.url(apiUrl + path).withHeaders("User-Agent" -> UserAgent)
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

    def POST(
      path: String,
      data: play.api.libs.json.JsValue = play.api.libs.json.Json.obj(),
      q: Seq[(String, String)] = Seq.empty
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("POST", _requestHolder(path).withQueryString(q:_*)).post(data)
    }

    def GET(
      path: String,
      q: Seq[(String, String)] = Seq.empty
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("GET", _requestHolder(path).withQueryString(q:_*)).get()
    }

    def PUT(
      path: String,
      data: play.api.libs.json.JsValue = play.api.libs.json.Json.obj(),
      q: Seq[(String, String)] = Seq.empty
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("PUT", _requestHolder(path).withQueryString(q:_*)).put(data)
    }

    def PATCH(
      path: String,
      data: play.api.libs.json.JsValue = play.api.libs.json.Json.obj(),
      q: Seq[(String, String)] = Seq.empty
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("PATCH", _requestHolder(path).withQueryString(q:_*)).patch(data)
    }

    def DELETE(
      path: String,
      q: Seq[(String, String)] = Seq.empty
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("DELETE", _requestHolder(path).withQueryString(q:_*)).delete()
    }

  }

  case class FailedRequest(response: play.api.libs.ws.WSResponse) extends Exception(response.status + ": " + response.body)

  package error {

    import referenceapi.models.json._

    case class ErrorsResponse(response: play.api.libs.ws.WSResponse) extends Exception(response.status + ": " + response.body) {

      lazy val errors = response.json.as[Seq[referenceapi.models.Error]]

    }
  }


}
