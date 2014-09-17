import com.ning.http.client._
import scala.concurrent._
import scala.concurrent.duration._

object Main extends App {

  import ExecutionContext.Implicits.global

  val client = new Foo.Client("http://localhost:8001")
  try {
    val result = Await.result(client.healthchecks.get(), 5 seconds)
    println("RESULT: " + result)
  } finally {
    client.asyncHttpClient.close()
  }

}

class Logger() {

  def info(message: String) {
    println(message)
  }

}

object Foo {
  class Client(
    apiUrl: String,
    apiToken: scala.Option[String] = None
  ) {
    val asyncHttpClient = new AsyncHttpClient()
    val logger = new Logger()

    def healthchecks: Healthchecks = Healthchecks

    def _logRequest(
      method: String,
      path: String,
      q: Seq[(String, String)] = Seq.empty
    ) {
      val queryComponents = for {
        (name, values) <- q
        value <- values
      } yield name -> value
      val url = s"${apiUrl}${path}${queryComponents.mkString("?", "&", "")}"
      apiToken.fold(logger.info(s"curl -X $method $url")) { _ =>
        logger.info(s"curl -X $method -u '[REDACTED]:' $url")
      }
    }

    def GET(
      url: String,
      q: Seq[(String, String)] = Seq.empty
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Response] = {
      _logRequest("GET", url, q)
      // TODO: Query parameters

      val result = Promise[Response]()
      asyncHttpClient.prepareGet(url).execute(
        new AsyncCompletionHandler[Unit]() {
          override def onCompleted(r: Response) = result.success(r)
        }
      )
      result.future
    }

    object Healthchecks extends Healthchecks {
      import com.gilt.quality.models.json.jsonReadsQualityHealthcheck

      override def get()(implicit ec: scala.concurrent.ExecutionContext):
          scala.concurrent.Future[scala.Option[com.gilt.quality.models.Healthcheck]] =
      {
        val url = s"$apiUrl/_internal_/healthcheck"
        GET(url).map { r =>
          case r if r.status == 200 => {
            play.api.libs.json.Json.parse(r.getResponseBody("UTF-8")).validate[com.gilt.quality.models.Healthcheck] match {
              case play.api.libs.json.JsSuccess(x, _) => Some(x)
              case play.api.libs.json.JsError(errors) => {
                throw new FailedRequest(r, Some("Invalid json for com.gilt.quality.models.Healthcheck: " + errors.mkString(" ")))
              }
            }
          }
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
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
