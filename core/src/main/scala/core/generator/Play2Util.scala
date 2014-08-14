package com.gilt.svcavroschemaregistry.models {
  case class Errors(
    messages: scala.collection.Seq[String]
  )

  /**
   * An Avro schema together with it's subject and fingerprint
   */
  case class SchemaDetails(
    subject: String,
    fingerprint: String,
    schema: String
  )

  /**
   * A single event data type, which may evolve through multiple schemas
   */
  case class Subject(
    subject: String
  )


}

package com.gilt.svcavroschemaregistry.models {
  package object json {
    import play.api.libs.json.__
    import play.api.libs.json.JsString
    import play.api.libs.json.Writes
    import play.api.libs.functional.syntax._

    private[svcavroschemaregistry] implicit val jsonReadsUUID = __.read[String].map(java.util.UUID.fromString)

    private[svcavroschemaregistry] implicit val jsonWritesUUID = new Writes[java.util.UUID] {
      def writes(x: java.util.UUID) = JsString(x.toString)
    }

    private[svcavroschemaregistry] implicit val jsonReadsJodaDateTime = __.read[String].map { str =>
      import org.joda.time.format.ISODateTimeFormat.dateTimeParser
      dateTimeParser.parseDateTime(str)
    }

    private[svcavroschemaregistry] implicit val jsonWritesJodaDateTime = new Writes[org.joda.time.DateTime] {
      def writes(x: org.joda.time.DateTime) = {
        import org.joda.time.format.ISODateTimeFormat.dateTime
        val str = dateTime.print(x)
        JsString(str)
      }
    }


    implicit def jsonReadssvcavroschemaregistryErrors: play.api.libs.json.Reads[Errors] = {
      (__ \ "messages").readNullable[scala.collection.Seq[String]].map(_.getOrElse(Nil)).map { x => new Errors(messages = x) }
    }

    implicit def jsonWritessvcavroschemaregistryErrors: play.api.libs.json.Writes[Errors] = new play.api.libs.json.Writes[Errors] {
      def writes(x: Errors) = play.api.libs.json.Json.obj(
        "messages" -> play.api.libs.json.Json.toJson(x.messages)
      )
    }

    implicit def jsonReadssvcavroschemaregistrySchemaDetails: play.api.libs.json.Reads[SchemaDetails] = {
      (
        (__ \ "subject").read[String] and
        (__ \ "fingerprint").read[String] and
        (__ \ "schema").read[String]
      )(SchemaDetails.apply _)
    }

    implicit def jsonWritessvcavroschemaregistrySchemaDetails: play.api.libs.json.Writes[SchemaDetails] = {
      (
        (__ \ "subject").write[String] and
        (__ \ "fingerprint").write[String] and
        (__ \ "schema").write[String]
      )(unlift(SchemaDetails.unapply _))
    }

    implicit def jsonReadssvcavroschemaregistrySubject: play.api.libs.json.Reads[Subject] = {
      (__ \ "subject").read[String].map { x => new Subject(subject = x) }
    }

    implicit def jsonWritessvcavroschemaregistrySubject: play.api.libs.json.Writes[Subject] = new play.api.libs.json.Writes[Subject] {
      def writes(x: Subject) = play.api.libs.json.Json.obj(
        "subject" -> play.api.libs.json.Json.toJson(x.subject)
      )
    }
  }
}

package com.gilt.svcavroschemaregistry {
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
    import com.gilt.svcavroschemaregistry.models._
    import com.gilt.svcavroschemaregistry.models.json._

    private val logger = play.api.Logger("com.gilt.svcavroschemaregistry.client")

    logger.info(s"Initializing com.gilt.svcavroschemaregistry.client for url $apiUrl")

    def subjects: Subjects = Subjects

    trait Subjects {
      /**
       * Gets all subjects which have a schema registered.
       */
      def get()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[com.gilt.svcavroschemaregistry.models.Subject]]

      /**
       * Registers a new schema version for the specified subject. The body should
       * contain a text representation of the schema (not a SchemaDetails).  A new schema
       * which violates evolution rules will result in a 409-Conflict response.
       */
      def putSchemasBySubject(value: String, 
        subject: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.svcavroschemaregistry.models.SchemaDetails]

      /**
       * Gets a list of all schemas registered for this subject.
       */
      def getSchemasBySubject(
        subject: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[com.gilt.svcavroschemaregistry.models.SchemaDetails]]

      /**
       * Gets the schema with the specified fingerprint
       */
      def getSchemasBySubjectAndFingerprint(
        subject: String,
        fingerprint: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.svcavroschemaregistry.models.SchemaDetails]]

      /**
       * Gets the latest schema for the specified subject
       */
      def getLatestSchemaBySubject(
        subject: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.svcavroschemaregistry.models.SchemaDetails]]
    }

    object Subjects extends Subjects {
      override def get()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[com.gilt.svcavroschemaregistry.models.Subject]] = {
        GET(s"/subjects").map {
          case r if r.status == 200 => r.json.as[scala.collection.Seq[com.gilt.svcavroschemaregistry.models.Subject]]
          case r => throw new FailedRequest(r)
        }
      }

      override def putSchemasBySubject(value: String, 
        subject: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.svcavroschemaregistry.models.SchemaDetails] = {
        val payload = play.api.libs.json.Json.toJson(value)

        PUT(s"/subjects/${play.utils.UriEncoding.encodePathSegment(subject, "UTF-8")}/schemas", payload).map {
          case r if r.status == 201 => r.json.as[com.gilt.svcavroschemaregistry.models.SchemaDetails]
          case r if r.status == 409 => throw new com.gilt.svcavroschemaregistry.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def getSchemasBySubject(
        subject: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[com.gilt.svcavroschemaregistry.models.SchemaDetails]] = {
        GET(s"/subjects/${play.utils.UriEncoding.encodePathSegment(subject, "UTF-8")}/schemas").map {
          case r if r.status == 200 => r.json.as[scala.collection.Seq[com.gilt.svcavroschemaregistry.models.SchemaDetails]]
          case r => throw new FailedRequest(r)
        }
      }

      override def getSchemasBySubjectAndFingerprint(
        subject: String,
        fingerprint: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.svcavroschemaregistry.models.SchemaDetails]] = {
        GET(s"/subjects/${play.utils.UriEncoding.encodePathSegment(subject, "UTF-8")}/schemas/${play.utils.UriEncoding.encodePathSegment(fingerprint, "UTF-8")}").map {
          case r if r.status == 200 => Some(r.json.as[com.gilt.svcavroschemaregistry.models.SchemaDetails])
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }

      override def getLatestSchemaBySubject(
        subject: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.svcavroschemaregistry.models.SchemaDetails]] = {
        GET(s"/subjects/${play.utils.UriEncoding.encodePathSegment(subject, "UTF-8")}/latestSchema").map {
          case r if r.status == 200 => Some(r.json.as[com.gilt.svcavroschemaregistry.models.SchemaDetails])
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
    }

    private val UserAgent = "apidoc:0.5.0 http://www.apidoc.me/gilt/code/svc-avro-schema-registry/0.0.1-dev/play_2_3_client"

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

    import com.gilt.svcavroschemaregistry.models.json._

    case class ErrorsResponse(response: play.api.libs.ws.WSResponse) extends Exception(response.status + ": " + response.body) {

      lazy val errors = response.json.as[com.gilt.svcavroschemaregistry.models.Errors]

    }
  }


}
