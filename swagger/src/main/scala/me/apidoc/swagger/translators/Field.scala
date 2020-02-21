package me.apidoc.swagger.translators

import lib.Primitives
import me.apidoc.swagger.Util
import io.apibuilder.spec.v0.{ models => apidoc }
import io.swagger.{ models => swagger }
import io.swagger.models.properties._
import scala.jdk.CollectionConverters._

object Field {

  def apply(
    resolver: Resolver,
    modelName: String,
    name: String,
    prop: Property
  ): apidoc.Field = {
    // Ignoring:
    // Option(prop.getXml)
    // Option(prop.getReadOnly)
    val base = apidoc.Field(
      name = name,
      `type` = resolver.schemaType(prop),
      description = Option(prop.getDescription),
      required = prop.getRequired(),
      example = Option(prop.getExample()).map(_.toString),
      attributes = Util.vendorExtensionsToAttributes(prop.getVendorExtensions)
    )
    specialize(base, prop, modelName)
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
    prop: Property,
    modelName: String
  ): apidoc.Field = {
    prop match {

      case p: ArrayProperty => {
        base.copy(
          description = Option(p.getUniqueItems()).getOrElse(false) match {
            case true => Util.combine(Seq(base.description, Some(s"Note: items are unique")))
            case false => base.description
          }
        )
      }

      case p: BooleanProperty => {
        base.copy(
          `type` = Primitives.Boolean.toString
        )
      }

      case p: DateProperty => {
        base.copy(
          `type` = Primitives.DateIso8601.toString
        )
      }

      case p: DateTimeProperty => {
        base.copy(
          `type` = Primitives.DateTimeIso8601.toString
        )
      }

      case p: AbstractNumericProperty => {
        // Also covers DecimalProperty, DoubleProperty, FloatProperty, IntegerProperty, LongProperty
        base.copy(
          minimum = Option(p.getMinimum()).map(_.longValue) match {
            case None => Option(p.getMinimum()).map(_.longValue)
            case Some(v) => Some(v)
          },
          maximum = Option(p.getMaximum()).map(_.longValue) match {
            case None => Option(p.getMaximum()).map(_.longValue)
            case Some(v) => Some(v)
          }
        )
      }

      case p: FileProperty => {
        // TODO: we need a datatype for File
        base
      }

      case p: MapProperty => {
        // TODO: p.getAdditionalProperties
        base.copy(
          `type` = "map[" + base.`type` + "]"
        )
      }

      case p: RefProperty => {
        base.copy(
          `type` = p.getSimpleRef
        )
      }

      case p: StringProperty => {
        if(p.getEnum!=null && !p.getEnum.isEmpty){
          stringProperty(base, None, None, None).copy(
            `type` = Util.buildPropertyEnumTypeName(modelName, base.name)
          )
        } else {
          stringProperty(base, Option(p.getMinLength), Option(p.getMaxLength), Option(p.getPattern)).copy(
            `type` = Primitives.String.toString
          )
        }

      }

      case p: UUIDProperty => {
        stringProperty(base, Option(p.getMinLength), Option(p.getMaxLength), Option(p.getPattern)).copy(
          `type` = Primitives.Uuid.toString
        )
      }

      case _ => {
        base
      }
    }
  }

  private def stringProperty(
    base: apidoc.Field,
    minimum: Option[Integer],
    maximum: Option[Integer],
    pattern: Option[String]
  ): apidoc.Field = {
    base.copy(
      minimum = minimum.map(_.toLong),
      maximum = maximum.map(_.toLong),
      description = pattern match {
        case None => base.description
        case Some(pattern) => Util.combine(Seq(base.description, Some(s"Pattern: $pattern")))
      }
    )
  }

}
