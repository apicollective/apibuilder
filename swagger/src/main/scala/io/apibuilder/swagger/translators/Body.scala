package io.apibuilder.swagger.translators

import io.apibuilder.spec.v0.{ models => apidoc }
import io.swagger.models.{ArrayModel, ModelImpl, RefModel}
import io.swagger.models.{ parameters => swaggerParams }
import io.apibuilder.swagger.Util

object Body {

  def apply(
    resolver: Resolver,
    param: swaggerParams.BodyParameter
  ): apidoc.Body = {
    val bodyType = param.getSchema match {
      case a: ArrayModel => s"[${Field(resolver, param.getName, "", a.getItems).`type`}]"
      case m: ModelImpl => m.getType
      case m: RefModel => resolver.resolveWithError(m).name
      case _ => sys.error("Unsupported body type: " + param.getSchema.getClass)
    }

    apidoc.Body(
      `type` = bodyType,
      description = Option(param.getDescription),
      deprecation = None,
      attributes = Util.vendorExtensionsToAttributes(param.getVendorExtensions)
    )
  }

}
