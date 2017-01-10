package me.apidoc.swagger.translators

import lib.Primitives
import com.bryzek.apidoc.spec.v0.{ models => apidoc }
import io.swagger.{ models => swagger }

object Response {

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
      deprecation = None
    )
  }

}
