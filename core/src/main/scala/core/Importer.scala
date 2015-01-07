package core

import com.gilt.apidocspec.models.Service
import com.gilt.apidocspec.models.json._
import play.api.libs.json.Json
import java.net.URI
import scala.util.{Failure, Success, Try}

case class Importer(uri: String) {

  private val client = new com.gilt.apidoc.Client(uri)

  lazy val service: Service = fetched match {
    case Left(error) => sys.error(s"Error fetching uri[$uri]: $error")
    case Right(service) => service
  }

  lazy val validate: Seq[String] = fetched match {
    case Left(error) => Seq(error)
    case Right(_) => Seq.empty
  }

  lazy val fetched: Either[String, Service] = {
    Try(
      fetchContents()
    ) match {
      case Success(service) => Right(service)
      case Failure(ex) => Left(ex.toString)
    }
  }

  def fetchContents(): Service = {
    if (uri.trim.toLowerCase.startsWith("file://")) {
      fetchContentsFromFile()
    } else {
      fetchContentsFromUri()
    }
  }

  private def fetchContentsFromFile(): Service = {
    val contents = scala.io.Source.fromURI(new URI(uri)).getLines.mkString
    Json.parse(contents).as[Service]
  }

  private def fetchContentsFromUri(): Service = {
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.Await
    import scala.concurrent.duration._

    Await.result(
      client._executeRequest("GET", "").map {
        case r if r.status == 200 => r.json.as[Service]
        case r => throw new com.gilt.apidoc.FailedRequest(r)
      },
      1000.millis
    )
  }

}
