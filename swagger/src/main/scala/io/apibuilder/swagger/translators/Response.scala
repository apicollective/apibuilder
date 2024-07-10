package io.apibuilder.swagger.translators

import lib.Primitives

import io.apibuilder.spec.v0.{models => apibuilder}
import io.apibuilder.swagger.Util
import io.swagger.{models => swagger}
import lib.Primitives

import scala.annotation.nowarn

object Response {

  @nowarn
  def apply(
    resolver: Resolver,
    code: String,
    response: swagger.Response
  ): apibuilder.Response = {
    // getExamples
    // getHeaders

    apibuilder.Response(
      code = code,
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
