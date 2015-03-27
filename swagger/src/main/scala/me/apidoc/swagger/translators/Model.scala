package me.apidoc.swagger.translators

import lib.Text
import me.apidoc.swagger.Util
import com.gilt.apidoc.spec.v0.{ models => apidoc }
import com.wordnik.swagger.{ models => swagger }

object Model {

  def apply(
    resolver: Resolver,
    name: String,
    m: swagger.ModelImpl
  ): apidoc.Model = {
    // TODO println("  - type: " + Option(schema.getType()))
    // TODO println("  - discriminator: " + Option(schema.getDiscriminator()))
    Option(m.getAdditionalProperties()).map { prop =>
      // TODO: println("    - additional property: " + prop)
    }

    apidoc.Model(
      name = name,
      plural = Text.pluralize(name),
      description = Util.combine(
        Seq(
          Option(m.getDescription()),
          ExternalDoc(Option(m.getExternalDocs))
        )
      ),
      deprecation = None,
      fields = Util.toMap(m.getProperties).map {
        case (key, prop) => Field(resolver, key, prop)
      }.toSeq
    )
  }

}
