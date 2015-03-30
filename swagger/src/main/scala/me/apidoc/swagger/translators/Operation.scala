package me.apidoc.swagger.translators

import me.apidoc.swagger.Util
import com.gilt.apidoc.spec.v0.{ models => apidoc }
import com.wordnik.swagger.{ models => swagger }
import com.wordnik.swagger.models.parameters.BodyParameter

object Operation {

  def apply(
    resolver: Resolver,
    method: apidoc.Method,
    url: String,
    op: com.wordnik.swagger.models.Operation
  ): apidoc.Operation = {
    val summary = Option(op.getSummary())
    val description = Option(op.getDescription())

    val parameters = Util.toArray(op.getParameters).flatMap { param =>
      param match {
        case p: BodyParameter => None
        case _ => Some(Parameter(resolver, param))
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
        case true => Some(apidoc.Deprecation())
      },
      body = bodies.toList match {
        case Nil => None
        case body :: Nil => Some(body)
        case multiple => {
          sys.error("Multiple body parameters specified for operation at url[$url]")
        }
      },
      parameters = parameters,
      responses = Util.toMap(op.getResponses).map {
        case (code, swaggerResponse) => Response(resolver, code, swaggerResponse)
      }.toSeq
    )
  }

}
