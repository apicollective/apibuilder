import com.ning.http.client._
import scala.concurrent._
import scala.concurrent.duration._

object Main extends App {

  import ExecutionContext.Implicits.global

  val client = new Foo.Client("http://localhost:8001")
  try {
    println("healthcheck: " + Await.result(client.healthchecks.get(), 5 seconds).get)

    val incidents = Await.result(client.incidents.get(limit = Some(5)), 5 seconds)
    println("incidents: " + incidents.size)
    incidents.foreach { i =>
      println(" - " + i.id.toString)
    }

  } finally {
    client.asyncHttpClient.close()
  }

}

object Foo {
  class Client(
    apiUrl: String,
    apiToken: scala.Option[String] = None
  ) {
    import com.gilt.quality.models.json._

    val asyncHttpClient = new AsyncHttpClient()
    val UserAgent = "TODO"

    def healthchecks: Healthchecks = Healthchecks

    def incidents: Incidents = Incidents

    def _requestBuilder(method: String, path: String): RequestBuilder = {
      val builder = new RequestBuilder(method)
        .setUrl(apiUrl + path)
        .addHeader("User-Agent", UserAgent)

      apiToken.fold(builder) { token =>
        builder.setRealm(
          new Realm.RealmBuilder()
            .setPrincipal(token)
            .setScheme(Realm.AuthScheme.BASIC)
            .build()
        )
      }
    }

    def _logRequest(request: Request) {
      println("_logRequest: " + request)
    }

    def _executeRequest(
      method: String,
      path: String,
      queryParameters: Seq[(String, String)] = Seq.empty,
      formParameters: Seq[(String, String)] = Seq.empty,
      body: Option[play.api.libs.json.JsValue] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response] = {
      val request = _requestBuilder(method, path)

      queryParameters.foreach { pair =>
        request.addQueryParameter(pair._1, pair._2)
      }

      queryParameters.foreach { pair =>
        request.addParameter(pair._1, pair._2)
      }

      val requestWithParamsAndBody = body.fold(request) { b =>
        val serialized = play.api.libs.json.Json.stringify(b)
        request.setBody(serialized).addHeader("Content-type", "application/json")
      }

      val finalRequest = requestWithParamsAndBody.build()
      _logRequest(finalRequest)

      val result = Promise[Response]()
      asyncHttpClient.executeRequest(finalRequest,
        new AsyncCompletionHandler[Unit]() {
          override def onCompleted(r: Response) = result.success(r)
        }
      )
      result.future
    }

    object Healthchecks extends Healthchecks {
      override def get()(implicit ec: scala.concurrent.ExecutionContext):
          scala.concurrent.Future[scala.Option[com.gilt.quality.models.Healthcheck]] =
      {
        _executeRequest("GET", "/_internal_/healthcheck").map {
          case r if r.getStatusCode == 200 => {
            play.api.libs.json.Json.parse(r.getResponseBody("UTF-8")).validate[com.gilt.quality.models.Healthcheck] match {
              case play.api.libs.json.JsSuccess(x, _) => Some(x)
              case play.api.libs.json.JsError(errors) => {
                throw new FailedRequest(r, Some("Invalid json: " + errors.mkString(" ")))
              }
            }
          }
          case r if r.getStatusCode == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
    }

    object Incidents extends Incidents {
      override def get(
        id: scala.Option[Long] = None,
        teamKey: scala.Option[String] = None,
        hasTeam: scala.Option[Boolean] = None,
        hasPlan: scala.Option[Boolean] = None,
        hasGrade: scala.Option[Boolean] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[com.gilt.quality.models.Incident]] = {
        val query = Seq(
          id.map("id" -> _.toString),
          teamKey.map("team_key" -> _),
          hasTeam.map("has_team" -> _.toString),
          hasPlan.map("has_plan" -> _.toString),
          hasGrade.map("has_grade" -> _.toString),
          limit.map("limit" -> _.toString),
          offset.map("offset" -> _.toString)
        ).flatten

        _executeRequest("GET", s"/incidents", query).map {
          case r if r.getStatusCode == 200 => {
            play.api.libs.json.Json.parse(r.getResponseBody("UTF-8")).validate[scala.collection.Seq[com.gilt.quality.models.Incident]] match {
              case play.api.libs.json.JsSuccess(x, _) => x
              case play.api.libs.json.JsError(errors) => {
                throw new FailedRequest(r, Some("Invalid json: " + errors.mkString(" ")))
              }
            }
          }
          case r => throw new FailedRequest(r)
        }
      }

      override def getById(
        id: Long
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.quality.models.Incident]] = {
        _executeRequest("GET", s"/incidents/${id}").map {
          case r if r.getStatusCode == 200 => {
            play.api.libs.json.Json.parse(r.getResponseBody("UTF-8")).validate[com.gilt.quality.models.Incident] match {
              case play.api.libs.json.JsSuccess(x, _) => Some(x)
              case play.api.libs.json.JsError(errors) => {
                throw new FailedRequest(r, Some("Invalid json: " + errors.mkString(" ")))
              }
            }
          }
          case r if r.getStatusCode == 404 => None
          case r => throw new FailedRequest(r)
        }
      }

      override def post(
        teamKey: scala.Option[String] = None,
        severity: String,
        summary: String,
        description: scala.Option[String] = None,
        tags: scala.collection.Seq[String] = Nil
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.quality.models.Incident] = {
        val payload = play.api.libs.json.Json.obj(
          "team_key" -> play.api.libs.json.Json.toJson(teamKey),
          "severity" -> play.api.libs.json.Json.toJson(severity),
          "summary" -> play.api.libs.json.Json.toJson(summary),
          "description" -> play.api.libs.json.Json.toJson(description),
          "tags" -> play.api.libs.json.Json.toJson(tags)
        )

        _executeRequest("POST", s"/incidents", body = Some(payload)).map {
          case r if r.getStatusCode == 201 => {
            play.api.libs.json.Json.parse(r.getResponseBody("UTF-8")).validate[com.gilt.quality.models.Incident] match {
              case play.api.libs.json.JsSuccess(x, _) => x
              case play.api.libs.json.JsError(errors) => {
                throw new FailedRequest(r, Some("Invalid json: " + errors.mkString(" ")))
              }
            }
          }
          // TODO case r if r.getStatusCode == 409 => throw new com.gilt.quality.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

/*
      override def putById(
        id: Long,
        teamKey: scala.Option[String] = None,
        severity: String,
        summary: String,
        description: scala.Option[String] = None,
        tags: scala.collection.Seq[String] = Nil
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.quality.models.Incident] = {
        val payload = play.api.libs.json.Json.obj(
          "team_key" -> play.api.libs.json.Json.toJson(teamKey),
          "severity" -> play.api.libs.json.Json.toJson(severity),
          "summary" -> play.api.libs.json.Json.toJson(summary),
          "description" -> play.api.libs.json.Json.toJson(description),
          "tags" -> play.api.libs.json.Json.toJson(tags)
        )

        PUT(s"/incidents/${id}", payload).map {
          case r if r.getStatusCode == 201 => r.json.as[com.gilt.quality.models.Incident]
          case r if r.getStatusCode == 409 => throw new com.gilt.quality.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def deleteById(
        id: Long
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]] = {
        DELETE(s"/incidents/${id}").map {
          case r if r.getStatusCode == 204 => Some(Unit)
          case r if r.getStatusCode == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
 */
    }

    trait Incidents {
      /**
       * Search all incidents. Results are always paginated.
       */
      def get(
        id: scala.Option[Long] = None,
        teamKey: scala.Option[String] = None,
        hasTeam: scala.Option[Boolean] = None,
        hasPlan: scala.Option[Boolean] = None,
        hasGrade: scala.Option[Boolean] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[com.gilt.quality.models.Incident]]

      /**
       * Returns information about the incident with this specific id.
       */
      def getById(
        id: Long
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.quality.models.Incident]]

      /**
       * Create a new incident.
       */
      def post(
        teamKey: scala.Option[String] = None,
        severity: String,
        summary: String,
        description: scala.Option[String] = None,
        tags: scala.collection.Seq[String] = Nil
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.quality.models.Incident]

      /**
       * Updates an incident.
       */
/*
      def putById(
        id: Long,
        teamKey: scala.Option[String] = None,
        severity: String,
        summary: String,
        description: scala.Option[String] = None,
        tags: scala.collection.Seq[String] = Nil
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.quality.models.Incident]

      def deleteById(
        id: Long
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]]
 */
    }

  }

  case class FailedRequest(
    response: Response,
    message: Option[String] = None
  ) extends Exception(message.getOrElse(response.getStatusCode() + ": " + response.getResponseBody("UTF-8")))

  trait Healthchecks {
    def get()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.quality.models.Healthcheck]]
  }

}
