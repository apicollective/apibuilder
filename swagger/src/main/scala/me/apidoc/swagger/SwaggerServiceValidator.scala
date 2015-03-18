package me.apidoc.swagger

import lib.{ServiceConfiguration, ServiceValidator}
import scala.util.{Failure, Success, Try}
import com.gilt.apidoc.spec.v0.models.Service

case class SwaggerServiceValidator(
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
