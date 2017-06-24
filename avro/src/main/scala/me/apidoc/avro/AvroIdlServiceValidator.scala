package me.apidoc.avro

import lib.{ServiceConfiguration, ServiceValidator}
import scala.util.{Failure, Success, Try}
import io.apibuilder.spec.v0.models.Service

case class AvroIdlServiceValidator(
  config: ServiceConfiguration,
  definition: String
) extends ServiceValidator[Service] {

  override def validate(): Either[Seq[String], Service] = {
    Try(Parser(config).parseString(definition)) match {
      case Failure(ex) => Left(Seq(ex.toString))
      case Success(service) => Right(service)
    }
  }

}
