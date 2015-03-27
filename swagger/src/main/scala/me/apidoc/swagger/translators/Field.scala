package me.apidoc.swagger.translators

import lib.Primitives
import com.gilt.apidoc.spec.v0.{ models => apidoc }
import com.wordnik.swagger.{ models => swagger }
import com.wordnik.swagger.models.properties.Property

object Field {

  def apply(
    resolver: Resolver,
    name: String,
    prop: Property
  ): apidoc.Field = {
    // Ignoring:
    // println(s"    - readOnly: " + Option(prop.getReadOnly()))
    // println(s"    - xml: " + Option(prop.getXml()))
    apidoc.Field(
      name = name,
      description = Option(prop.getDescription()),
      `type` = resolver.schemaType(prop),
      required = prop.getRequired(),
      example = Option(prop.getExample())
    )
  }


}
