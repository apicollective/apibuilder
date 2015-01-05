package core

import com.gilt.apidocspec.models.Service
import com.gilt.apidocspec.models.json._
import play.api.libs.json.Json
import java.net.URI
import scala.util.{Failure, Success, Try}

case class Import(uri: String) {

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
      fetchContents
    ) match {
      case Success(service) => Right(service)
      case Failure(ex) => Left(ex.toString)
    }
  }

  def fetchContents(): Service = {
    // val contents = scala.io.Source.fromURI(new URI(uri)).getLines.mkString
    // Json.parse(contents).as[Service]

    import play.api.Play.current
    import scala.concurrent.Await
    import scala.concurrent.duration._
    implicit val context = scala.concurrent.ExecutionContext.Implicits.global

    val holder = play.api.libs.ws.WS.url(uri) //.withHeaders("User-Agent" -> UserAgent)
    //apiToken.fold(holder) { token =>
    //  holder.withAuth(token, "", play.api.libs.ws.WSAuthScheme.BASIC)
    //}
    Await.result(
      holder.get().map( r =>
        r.json.as[Service]
      ),
      1000.millis
    )
  }

}
