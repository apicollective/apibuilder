package io.apibuilder.swagger.translators

import io.apibuilder.spec.v0.{models => apidoc}
import io.swagger.models.parameters.BodyParameter
import io.apibuilder.swagger.{SwaggerData, Util}

object Operation {

  def apply(
    resolver: Resolver,
    modelName: String,
    method: apidoc.Method,
    url: String,
    op: io.swagger.models.Operation
  ): apidoc.Operation = {
    val summary = Option(op.getSummary)
    val description = Option(op.getDescription)

    val parameters = Util.toArray(op.getParameters).flatMap { param =>
      param match {
        case _: BodyParameter => None
        case _ => Some(Parameter(resolver, modelName, method, param))
      }
    }

    val bodies = Util.toArray(op.getParameters).flatMap { param =>
      param match {
        case p: BodyParameter => Some(Body(resolver, p))
        case _ => None
      }
    }

    // Util.toArray(op.getSchemes()).mkString(", ")
    // Util.toArray(op.getConsumes()).mkString(", ")
    // Util.toArray(op.getProduces).mkString(", ")
    // tags: " + Util.toArray(op.getTags()).mkString(", ")
    // getSecurity
    // getVendorExtensions
    // getOperationId (this is like a nick name for the method - e.g. findPets)

    apidoc.Operation(
      method = method,
      path = Util.substitutePathParameters(url),
      description = Util.combine(Seq(summary, description, ExternalDoc(Option(op.getExternalDocs)))),
      deprecation = Option(op.isDeprecated).getOrElse(false) match {
        case false => None
        case _ => Some(apidoc.Deprecation())
      },
      body = bodies.toList match {
        case Nil => None
        case body :: Nil => Some(body)
        case multiple => {
          sys.error(s"Multiple (#${multiple.length}) body parameters specified for operation at url[$url]")
        }
      },
      parameters = parameters,
      responses = Util.toMap(op.getResponses).map {
        case (code, swaggerResponse) => Response(resolver, code, swaggerResponse)
      }.toSeq,
      attributes =
        Seq(
          SwaggerData(
            externalDocs = op.getExternalDocs,
            operationSecurity = op.getSecurity,
            schemes = op.getSchemes,
            summary = op.getSummary,
            operationId = op.getOperationId,
            tags = op.getTags
          ).toAttribute
        ).flatten ++ Util.vendorExtensionsToAttributes(op.getVendorExtensions)
    )
  }

}
