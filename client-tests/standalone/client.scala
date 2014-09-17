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
        asyncHttpClient.prepareGet(s"$apiUrl/_internal_/healthcheck").execute(
          new AsyncCompletionHandler[Unit]() {
            override def onCompleted(r: Response) = {
              import com.gilt.quality.models.json.jsonReadsQualityHealthcheck

              if (r.getStatusCode() == 200) {
                val body = r.getResponseBody("UTF-8")
                try {
                  val jsValue = play.api.libs.json.Json.parse(body)
                
                  jsValue.validate[com.gilt.quality.models.Healthcheck] match {
                    case play.api.libs.json.JsSuccess(x, _) => result.success(Some(x))
                    case play.api.libs.json.JsError(errors) => sys.error("Error parsing json: " + errors.mkString(" "))
                  }
                } catch {
                  case t: Throwable => result.failure(t)
                }
              } else {
                println("     code: " + r.getStatusCode())
                sys.error("TODO: code = " + r.getStatusCode())
              }

              ()
            }
          }
        )

        result.future
      }
    }
  }

  case class FailedRequest(e: Throwable) extends Exception(e.toString)

  trait Healthchecks {
    def get()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.quality.models.Healthcheck]]
  }

}
