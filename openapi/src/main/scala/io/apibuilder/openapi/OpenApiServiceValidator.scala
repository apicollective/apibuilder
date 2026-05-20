package io.apibuilder.openapi

import cats.data.ValidatedNec
import cats.implicits._
import io.apibuilder.spec.v0.models.Service
import lib.{ServiceConfiguration, ServiceValidator}

case class OpenApiServiceValidator(config: ServiceConfiguration) extends ServiceValidator[Service] {

  override def validate(rawInput: String): ValidatedNec[String, Service] =
    OpenApiParser.fromString(rawInput) match {
      case Left(err) => err.invalidNec
      case Right(openApi) => Converter.convert(openApi, config).validNec
    }
}
