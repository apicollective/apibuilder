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

object Foo {
  class Client(apiUrl: String, apiToken: scala.Option[String] = None) {
    val asyncHttpClient = new AsyncHttpClient()

    def healthchecks: Healthchecks = Healthchecks

    object Healthchecks extends Healthchecks {

      override def get()(implicit ec: scala.concurrent.ExecutionContext):
          scala.concurrent.Future[scala.Option[com.gilt.quality.models.Healthcheck]] =
      {
        val result = Promise[scala.Option[com.gilt.quality.models.Healthcheck]]()
        val url = s"$apiUrl/_internal_/healthcheck"
        asyncHttpClient.prepareGet(url).execute(
          new AsyncCompletionHandler[Unit]() {
            override def onCompleted(r: Response) = {
              import com.gilt.quality.models.json.jsonReadsQualityHealthcheck
              val body = r.getResponseBody("UTF-8")

              if (r.getStatusCode() == 200) {
                try {
                  play.api.libs.json.Json.parse(body).validate[com.gilt.quality.models.Healthcheck] match {
                    case play.api.libs.json.JsSuccess(x, _) => result.success(Some(x))
                    case play.api.libs.json.JsError(errors) => {
                      result.failure(
                        new FailedRequest(
                          url,
                          r,
                          Some("Invalid json for com.gilt.quality.models.Healthcheck: " + errors.mkString(" "))
                        )
                      )
                    }
                  }
                } catch {
                  case t: Throwable => result.failure(t)
                }

              } else if (r.getStatusCode() == 404) {
                result.success(None)

              } else {
                result.failure(new FailedRequest(url, r))
              }

              ()
            }
          }
        )

        result.future
      }
    }
  }

  case class FailedRequest(
    url: String,
    response: Response,
    message: Option[String] = None
  ) extends Exception(message.getOrElse(response.getStatusCode() + s" for url[$url]: " + response.getResponseBody("UTF-8")))

  trait Healthchecks {
    def get()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.quality.models.Healthcheck]]
  }

}
