package core

import com.bryzek.apidoc.spec.v0.models.Service
import scala.util.{Failure, Success, Try}

case class Importer(fetcher: ServiceFetcher, uri: String) {

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
      case Success(service) => Right(service)
      case Failure(ex) => Left(s"Error fetching uri[$uri]: ${ex}")
    }
  }

}
