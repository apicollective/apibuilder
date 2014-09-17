import com.ning.http.client._
import scala.concurrent._
import scala.concurrent.duration._

object Main extends App {

  import ExecutionContext.Implicits.global

  val client = new Foo.Client("http://localhost:8001")
  try {
    println("client: " + client)
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
        asyncHttpClient.prepareGet("http://localhost:8001/healthchecks").execute(
          new AsyncCompletionHandler[Unit]() {
            override def onCompleted(r: Response) = {
              println("Completed: " + r)
              val item: scala.Option[com.gilt.quality.models.Healthcheck] = Some(com.gilt.quality.models.Healthcheck("Healthy"))
              result.success(item)
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
