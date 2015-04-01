package me.apidoc.swagger.translators

import com.gilt.apidoc.spec.v0.{ models => apidoc }
import com.wordnik.swagger.models.{ModelImpl, RefModel}
import com.wordnik.swagger.models.{ parameters => swaggerParams }

object Body {

  def apply(
    resolver: Resolver,
    param: swaggerParams.BodyParameter
  ): apidoc.Body = {
    val bodyType = param.getSchema match {
      case m: ModelImpl => m.getType
      case m: RefModel => resolver.resolveWithError(m).name
      case _ => sys.error("Unsupported body type: " + param.getSchema.getClass)
    }

    apidoc.Body(
      `type` = bodyType,
      description = Option(param.getDescription),
      deprecation = None
    )
  }

}
