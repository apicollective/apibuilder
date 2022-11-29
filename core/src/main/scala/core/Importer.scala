package core

import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import cats.data.ValidatedNec
import io.apibuilder.spec.v0.models.Service

import scala.util.{Failure, Success, Try}

case class Importer(fetcher: ServiceFetcher, uri: String) {

  lazy val service: Service = fetched match {
    case Invalid(errors) => sys.error(s"Error fetching uri[$uri]: ${errors.toNonEmptyList.toList.mkString(", ")}")
    case Valid(service) => service
  }

  lazy val validate: ValidatedNec[String, Unit] = fetched.map(_ => ())

  lazy val fetched: ValidatedNec[String, Service] = {
    Try(
      fetcher.fetch(uri)
    ) match {
      case Success(service) => service.validNec
      case Failure(ex) => s"Error fetching uri[$uri]: ${ex}".invalidNec
    }
  }

}
