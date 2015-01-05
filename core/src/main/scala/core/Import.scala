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
      Json.parse(fetchContents).as[Service]
    ) match {
      case Success(service) => Right(service)
      case Failure(ex) => Left(ex.toString)
    }
  }

  def fetchContents(): String = {
    scala.io.Source.fromURI(new URI(uri)).getLines.mkString
  }

}
