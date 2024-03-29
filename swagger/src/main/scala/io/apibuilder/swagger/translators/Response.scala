package io.apibuilder.swagger.translators

import lib.Primitives
import io.apibuilder.spec.v0.{models => apidoc}
import io.apibuilder.swagger.Util
import io.swagger.{models => swagger}

import scala.annotation.nowarn

object Response {

  @nowarn
  def apply(
    resolver: Resolver,
    code: String,
    response: swagger.Response
  ): apidoc.Response = {
    val responseCode = if (code == "default") {
      apidoc.ResponseCodeOption.Default
    } else {
      apidoc.ResponseCodeInt(code.toInt)
    }

    // getExamples
    // getHeaders

    apidoc.Response(
      code = responseCode,
      `type` = Option(response.getSchema) match {
        case None => Primitives.Unit.toString
        case Some(schema) => resolver.schemaType(schema)
      },
      description = Option(response.getDescription),
      deprecation = None,
      attributes = Util.vendorExtensionsToAttributesOpt(response.getVendorExtensions)
    )
  }

}
