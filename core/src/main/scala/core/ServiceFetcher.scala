package core

import com.gilt.apidoc.v0.errors.FailedRequest
import com.gilt.apidoc.spec.v0.models.{Method, Service}
import com.gilt.apidoc.spec.v0.models.json._
import play.api.libs.json.Json
import java.net.URI
import com.fasterxml.jackson.core.{JsonParseException, JsonProcessingException}
import scala.util.{Failure, Success, Try}

trait ServiceFetcher {

  def fetch(uri: String): Service

}

case class ClientFetcher() extends ServiceFetcher {

  override def fetch(uri: String): Service = {
    if (uri.trim.toLowerCase.startsWith("file://")) {
      fetchContentsFromFile(uri)
    } else {
      fetchContentsFromUri(uri)
    }
  }

  private def fetchContentsFromFile(uri: String): Service = {
    val contents = scala.io.Source.fromURI(new URI(uri)).getLines.mkString
    Json.parse(contents).as[Service]
  }

  private def fetchContentsFromUri(uri: String): Service = {
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.Await
    import scala.concurrent.duration._

    val client = new com.gilt.apidoc.v0.Client(uri)

    Await.result(
      client._executeRequest(Method.Get.toString, "").map {
        case r if r.status == 200 => {
          try {
            r.json.as[Service]
          } catch {
            case e: JsonParseException => {
              throw new FailedRequest(r.status, s"Import Uri[$uri] did not return valid JSON")
            }
            case e: JsonProcessingException => {
              throw new FailedRequest(r.status, s"Import Uri[$uri] did not return valid JSON")
            }
          }
        }
        case r => throw new FailedRequest(r.status, "Expected HTTP 200 but receieved HTTP ${r.status}")
      },
      1000.millis
    )
  }
}
