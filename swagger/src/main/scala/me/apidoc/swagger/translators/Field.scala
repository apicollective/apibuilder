package me.apidoc.swagger.translators

import lib.Primitives
import me.apidoc.swagger.Util
import com.gilt.apidoc.spec.v0.{ models => apidoc }
import com.wordnik.swagger.{ models => swagger }
import com.wordnik.swagger.models.properties.{AbstractNumericProperty, ArrayProperty, Property, RefProperty, StringProperty, UUIDProperty}

object Field {

  def apply(
    resolver: Resolver,
    name: String,
    prop: Property
  ): apidoc.Field = {
    // Ignoring:
    // println(s"    - readOnly: " + Option(prop.getReadOnly()))
    // println(s"    - xml: " + Option(prop.getXml()))
    val base = apidoc.Field(
      name = name,
      description = Option(prop.getDescription()),
      `type` = resolver.schemaType(prop),
      required = prop.getRequired(),
      example = Option(prop.getExample())
    )
    specialize(base, prop)
  }

  def compose(f1: apidoc.Field, f2: apidoc.Field): apidoc.Field = {
    f1.copy(
      `type` = f2.`type`,
      description = Util.choose(f2.description, f1.description),
      deprecation = Util.choose(f2.deprecation, f1.deprecation),
      default = Util.choose(f2.default, f1.default),
      required = f2.required,
      minimum = Util.choose(f2.minimum, f1.minimum),
      maximum = Util.choose(f2.maximum, f1.maximum),
      example = Util.choose(f2.example, f1.example)
    )
  }


  private def specialize(
    base: apidoc.Field,
    prop: Property
  ): apidoc.Field = {
    prop match {
      case p: ArrayProperty => {
        base.copy(
          `type` = "[" + base.`type` + "]",
          description = Option(p.getUniqueItems()).getOrElse(false) match {
            case true => Util.combine(Seq(base.description, Some(s"Note: items are unique")))
            case false => base.description
          }
        )
      }
      case p: AbstractNumericProperty => {
        base.copy(
          minimum = Option(p.getMinimum()).map(_.toLong) match {
            case None => Option(p.getExclusiveMinimum()).map(_.toLong)
            case Some(v) => Some(v)
          },
          maximum = Option(p.getMaximum()).map(_.toLong) match {
            case None => Option(p.getExclusiveMaximum()).map(_.toLong)
            case Some(v) => Some(v)
          }
        )
      }

      case p: UUIDProperty => {
        base.copy(
          minimum = Option(p.getMinLength()).map(_.toLong),
          maximum = Option(p.getMaxLength()).map(_.toLong)
        )
      }

      case p: StringProperty => {
        base.copy(
          minimum = Option(p.getMinLength()).map(_.toLong),
          maximum = Option(p.getMaxLength()).map(_.toLong),
          description = Option(p.getPattern) match {
            case None => base.description
            case Some(pattern) => Util.combine(Seq(base.description, Some(s"Pattern: $pattern")))
          }
        )
      }
      case _ => base
    }
  }

}
