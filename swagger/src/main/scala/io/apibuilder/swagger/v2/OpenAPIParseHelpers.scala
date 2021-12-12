package io.apibuilder.swagger.v2

import io.apibuilder.spec.v0.models.Deprecation

trait OpenAPIParseHelpers {
  def trimmedString(value: String): Option[String] = Option(value).map(_.trim).filter(_.nonEmpty)

  def optionalLong(value: java.math.BigDecimal): Option[Long] = {
    Option(value).map(_.longValueExact())
  }

  def optionalLong(value: java.lang.Integer): Option[Long] = {
    Option(value).map(_.longValue())
  }

  def deprecation[T](deprecated: java.lang.Boolean): Option[Deprecation] = {
    Option(deprecated) match {
      case Some(v) if v => Some(Deprecation())
      case _ => None
    }
  }
}