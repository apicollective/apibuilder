package com.gilt.apidocgenerator {

  class Client(apiUrl: String, apiToken: scala.Option[String] = None) {
    import com.gilt.apidocgenerator.models.json._

    private val UserAgent = "apidoc:0.6.4 http://www.apidoc.me/gilt/code/apidoc-generator/0.7.1-dev/play_2_3_client"
    private val logger = play.api.Logger("com.gilt.apidocgenerator.client")

    logger.info(s"Initializing com.gilt.apidocgenerator.client for url $apiUrl")

    def generators: Generators = Generators

    object Generators extends Generators {
      override def get()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[com.gilt.apidocgenerator.models.Generator]] = {
        _executeRequest("GET", s"/generators").map {
          case r if r.status == 200 => r.json.as[scala.collection.Seq[com.gilt.apidocgenerator.models.Generator]]
          case r => throw new FailedRequest(r)
        }
      }

      override def getByKey(
        key: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidocgenerator.models.Generator]] = {
        _executeRequest("GET", s"/generators/${play.utils.UriEncoding.encodePathSegment(key, "UTF-8")}").map {
          case r if r.status == 200 => Some(r.json.as[com.gilt.apidocgenerator.models.Generator])
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }

      override def postExecuteByKey(serviceDescription: com.gilt.apidocgenerator.models.ServiceDescription,
        key: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[String] = {
        val payload = play.api.libs.json.Json.toJson(serviceDescription)

        _executeRequest("POST", s"/generators/${play.utils.UriEncoding.encodePathSegment(key, "UTF-8")}/execute", body = Some(payload)).map {
          case r if r.status == 200 => r.json.as[String]
          case r if r.status == 409 => throw new com.gilt.apidocgenerator.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }
    }

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

    def _executeRequest(
      method: String,
      path: String,
      queryParameters: Seq[(String, String)] = Seq.empty,
      body: Option[play.api.libs.json.JsValue] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      method.toUpperCase match {
        case "GET" => {
          _logRequest("GET", _requestHolder(path).withQueryString(queryParameters:_*)).get()
        }
        case "POST" => {
          _logRequest("POST", _requestHolder(path).withQueryString(queryParameters:_*)).post(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PUT" => {
          _logRequest("PUT", _requestHolder(path).withQueryString(queryParameters:_*)).put(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PATCH" => {
          _logRequest("PATCH", _requestHolder(path).withQueryString(queryParameters:_*)).patch(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "DELETE" => {
          _logRequest("DELETE", _requestHolder(path).withQueryString(queryParameters:_*)).delete()
        }
        case _ => {
          _logRequest(method, _requestHolder(path).withQueryString(queryParameters:_*))
          sys.error("Unsupported method[%s]".format(method))
        }
      }
    }

  }

  trait Generators {
    /**
     * Get all available generators
     */
    def get()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[com.gilt.apidocgenerator.models.Generator]]

    /**
     * Get the meta data of this generator
     */
    def getByKey(
      key: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidocgenerator.models.Generator]]

    /**
     * Invoke this generator
     */
    def postExecuteByKey(serviceDescription: com.gilt.apidocgenerator.models.ServiceDescription,
      key: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[String]
  }

  case class FailedRequest(
    response: play.api.libs.ws.WSResponse,
    message: Option[String] = None
  ) extends Exception(message.getOrElse(response.status + ": " + response.body))

  package error {

    import com.gilt.apidocgenerator.models.json._

    case class ErrorsResponse(
      response: play.api.libs.ws.WSResponse,
      message: Option[String] = None
    ) extends Exception(message.getOrElse(response.status + ": " + response.body)){
      import com.gilt.apidocgenerator.models.json._
      lazy val errors = response.json.as[scala.collection.Seq[com.gilt.apidocgenerator.models.Error]]
    }
  }

  object Bindables {

    import play.api.mvc.{PathBindable, QueryStringBindable}
    import org.joda.time.{DateTime, LocalDate}
    import org.joda.time.format.ISODateTimeFormat
    import com.gilt.apidocgenerator.models._

    // Type: date-time-iso8601
    implicit val pathBindableTypeDateTimeIso8601 = new PathBindable.Parsing[DateTime](
      ISODateTimeFormat.dateTimeParser.parseDateTime(_), _.toString, (key: String, e: Exception) => s"Error parsing date time $key. Example: 2014-04-29T11:56:52Z"
    )

    // Type: date-iso8601
    implicit val pathBindableTypeDateIso8601 = new PathBindable.Parsing[LocalDate](
      ISODateTimeFormat.yearMonthDay.parseLocalDate(_), _.toString, (key: String, e: Exception) => s"Error parsing date time $key. Example: 2014-04-29"
    )

    // Enum: HeaderType
    private val enumHeaderTypeNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${HeaderType.all.mkString(", ")}"

    implicit val pathBindableEnumHeaderType = new PathBindable.Parsing[HeaderType] (
      HeaderType.fromString(_).get, _.toString, enumHeaderTypeNotFound
    )

    implicit val queryStringBindableEnumHeaderType = new QueryStringBindable.Parsing[HeaderType](
      HeaderType.fromString(_).get, _.toString, enumHeaderTypeNotFound
    )

    // Enum: ParameterLocation
    private val enumParameterLocationNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${ParameterLocation.all.mkString(", ")}"

    implicit val pathBindableEnumParameterLocation = new PathBindable.Parsing[ParameterLocation] (
      ParameterLocation.fromString(_).get, _.toString, enumParameterLocationNotFound
    )

    implicit val queryStringBindableEnumParameterLocation = new QueryStringBindable.Parsing[ParameterLocation](
      ParameterLocation.fromString(_).get, _.toString, enumParameterLocationNotFound
    )

    // Enum: TypeKind
    private val enumTypeKindNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${TypeKind.all.mkString(", ")}"

    implicit val pathBindableEnumTypeKind = new PathBindable.Parsing[TypeKind] (
      TypeKind.fromString(_).get, _.toString, enumTypeKindNotFound
    )

    implicit val queryStringBindableEnumTypeKind = new QueryStringBindable.Parsing[TypeKind](
      TypeKind.fromString(_).get, _.toString, enumTypeKindNotFound
    )

  }

}
