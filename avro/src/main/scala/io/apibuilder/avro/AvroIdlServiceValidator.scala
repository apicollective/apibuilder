package io.apibuilder.avro

import cats.implicits._
import cats.data.ValidatedNec
import lib.{ServiceConfiguration, ServiceValidator}
import scala.util.{Failure, Success, Try}
import io.apibuilder.spec.v0.models.Service

case class AvroIdlServiceValidator(
  config: ServiceConfiguration
) extends ServiceValidator[Service] {

  override def validate(rawInput: String): ValidatedNec[String, Service] = {
    Try(Parser(config).parseString(rawInput)) match {
      case Failure(ex) => ex.toString.invalidNec
      case Success(service) => service.validNec
    }
  }

}
