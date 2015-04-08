package me.apidoc.swagger.translators

import lib.Primitives
import com.gilt.apidoc.spec.v0.{ models => apidoc }
import com.wordnik.swagger.{ models => swagger }

object Response {

  private val DefaultResponseCode = "default"

  def apply(
    resolver: Resolver,
    code: String,
    response: swagger.Response
  ): apidoc.Response = {
    val responseCode = if (code == DefaultResponseCode) {
      apidoc.StringWrapper(DefaultResponseCode)
    } else {
      apidoc.IntWrapper(code.toInt)
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
      deprecation = None
    )
  }

}
