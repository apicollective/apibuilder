package io.apibuilder.swagger.v2

trait OpenAPIParseHelpers {
  def trimmedString(value: String): Option[String] = Option(value).map(_.trim).filter(_.nonEmpty)
}