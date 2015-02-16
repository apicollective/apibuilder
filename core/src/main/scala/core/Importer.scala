package core

import com.gilt.apidoc.spec.v0.models.Service
import scala.util.{Failure, Success, Try}

case class Importer(fetcher: ServiceFetcher, uri: String) {

  println("FETCHER: " + fetcher)

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
      fetcher.fetch(uri)
    ) match {
      case Success(service) => println("success: " + service);Right(service)
      case Failure(ex) => Left(ex.getMessage)
    }
  }

}
