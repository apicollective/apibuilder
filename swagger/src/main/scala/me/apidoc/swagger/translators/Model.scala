package me.apidoc.swagger.translators

import com.bryzek.apidoc.spec.v0.models.EnumValue
import lib.Text
import me.apidoc.swagger.Util
import com.bryzek.apidoc.spec.v0.{models => apidoc}
import com.wordnik.swagger.models.properties.StringProperty
import com.wordnik.swagger.{models => swagger}

import scala.language.implicitConversions

object Model {

  def apply(
    resolver: Resolver,
    name: String,
    m: swagger.ModelImpl
  ): (apidoc.Model, Seq[apidoc.Enum]) = {
    // TODO println("  - type: " + Option(schema.getType()))
    // TODO println("  - discriminator: " + Option(schema.getDiscriminator()))
    Option(m.getAdditionalProperties()).map { prop =>
      // TODO: println("    - additional property: " + prop)
    }

    (apidoc.Model(
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
    ), enums(m))
  }

  private def enums(m: swagger.ModelImpl): Seq[apidoc.Enum] = {
    Util.toMap(m.getProperties).map {
      case (key, prop) => {
        prop match {
          case sp: StringProperty if(sp.getEnum!=null && !sp.getEnum.isEmpty) => {
              Some(apidoc.Enum(
                name = s"enum_${key}",
                plural = "",
                description = None,
                deprecation = None,
                values = Util.toArray(sp.getEnum).map { value =>
                  EnumValue(name = value, description = None, deprecation = None, attributes = Seq())},
                attributes = Seq()))
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
