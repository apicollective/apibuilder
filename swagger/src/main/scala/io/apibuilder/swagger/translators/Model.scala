package io.apibuilder.swagger.translators

import io.apibuilder.spec.v0.models.EnumValue
import io.apibuilder.spec.v0.{models => apidoc}
import io.swagger.{models => swagger}
import lib.Text
import io.apibuilder.swagger.{SwaggerData, Util}

object Model {
  val Placeholder: apidoc.Model = apidoc.Model(
    name = "placeholder",
    plural = "placeholders",
    fields = Seq(
     apidoc.Field(name = "placeholder", `type` = "string", required=false)
    )
  )

  def apply(
    resolver: Resolver,
    name: String,
    m: swagger.ModelImpl
  ): (Option[apidoc.Model], Seq[apidoc.Enum]) = {
    Util.isEnum(m) match {
      case true =>
        (None,
          Seq(
            apidoc.Enum(
              name = name,
              plural = Text.pluralize(name),
              description = Option(m.getDescription),
              deprecation = None,
              values = Util.toArray(m.getEnum).map { value =>
                EnumValue(name = value, description = None, deprecation = None, attributes = Seq())
              },
              attributes = Seq())
        ))
      case false =>
        (Some(apidoc.Model(
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
            case (key, prop) => Field(resolver, name, key, prop)
          }.toSeq,
          attributes =
            Seq(
              SwaggerData(
                externalDocs = m.getExternalDocs,
                example = m.getExample).toAttribute)
              .flatten ++ Util.vendorExtensionsToAttributes(m.getVendorExtensions)
        )), enums(m, name))
    }
  }

  private def enums(m: swagger.ModelImpl, apidocModelName: String): Seq[apidoc.Enum] = {
    Util.toMap(m.getProperties).map {
      case (name, prop) => {
        prop match {
          case sp: swagger.properties.StringProperty => {
            if(Util.hasStringEnum(sp)) {
              val enumTypeName = Util.buildPropertyEnumTypeName(apidocModelName, name)
              Some(apidoc.Enum(
                name = enumTypeName,
                plural = Text.pluralize(enumTypeName),
                description = None,
                deprecation = None,
                values = Util.toArray(sp.getEnum).map { value =>
                  EnumValue(name = value, description = None, deprecation = None, attributes = Seq())
                },
                attributes = Seq()))
            } else {
              None
            }
          }
          case _ => None
        }
      }
    }.toSeq.filter(_.isDefined).map(_.get)
  }

  def compose(m1: apidoc.Model, m2: apidoc.Model): apidoc.Model = {
    m1.copy(
      description = Util.choose(m2.description, m1.description),
      deprecation = Util.choose(m2.deprecation, m1.deprecation),
      fields = m1.fields.map { f =>
        m2.fields.find(_.name == f.name) match {
          case None => f
          case Some(other) => Field.compose(f, other)
        }
      } ++ m2.fields.filter( f => m1.fields.find(_.name == f.name).isEmpty )
    )
  }

}
