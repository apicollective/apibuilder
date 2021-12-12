package io.apibuilder.swagger.v2

import cats.implicits._
import cats.data.ValidatedNec
import io.apibuilder.spec.v0.models.Service
import io.swagger.v3.oas.models.OpenAPI
import lib.ServiceConfiguration
import io.swagger.v3.parser.OpenAPIV3Parser

import scala.jdk.CollectionConverters._
// import io.swagger.v3.parser.core.models.SwaggerParseResult
// import io.swagger.v3.oas.models.OpenAPI

case class V2Parser(config: ServiceConfiguration) {

  def parse(contents: String): ValidatedNec[String, Service] = {
    parseContents(contents).andThen { openApi =>
      parseOpenApi(openApi)
    }
  }

  private[this] def parseContents(contents: String): ValidatedNec[String, OpenAPI] = {
    val result = new OpenAPIV3Parser().readContents(contents, null, null)
    if (result.getMessages != null) {
      result.getMessages.asScala.mkString(", ").invalidNec
    } else {
      val openAPI = result.getOpenAPI
      if (openAPI == null) {
        "Unknown error parsing contents".invalidNec
      } else {
        openAPI.validNec
      }
    }
  }

  private[this] def parseOpenApi(api: OpenAPI): ValidatedNec[String, Service] = {
    s"TODO:${api.getInfo}".invalidNec
  }
}