package core

import lib.Primitives
import com.gilt.apidocgenerator.models._

case class TypeResolver(
  enumNames: Seq[String] = Seq.empty,
  modelNames: Seq[String] = Seq.empty
) {

  def toType(name: String): Option[Type] = {
    Primitives(name) match {
      case Some(pt) => Some(Type(TypeKind.Primitive, name))
      case None => {
        enumNames.find(_ == name) match {
          case Some(et) => Some(Type(TypeKind.Enum, name))
          case None => {
            modelNames.find(_ == name) match {
              case Some(mt) => Some(Type(TypeKind.Model, name))
              case None => None
            }
          }
        }
      }
    }
  }

  def toTypeInstance(internal: InternalParsedDatatype): Option[TypeInstance] = {
    toType(internal.name).map { TypeInstance(internal.container, _) }
  }

}


case class PrimitiveMetadata(
  primitive: Primitives,
  description: String,
  examples: Seq[String]
)

object PrimitiveMetadata {

  val All = Seq(
    PrimitiveMetadata(
      primitive = Primitives.Boolean,
      examples = TypeValidator.BooleanValues,
      description = "Represents a boolean value"
    ),

    PrimitiveMetadata(
      primitive = Primitives.DateIso8601,
      examples = Seq("2014-04-29"),
      description = "Date format in ISO 8601"
    ),

    PrimitiveMetadata(
      primitive = Primitives.DateTimeIso8601,
      examples = Seq("2014-04-29T11:56:52Z"),
      description = "Date time format in ISO 8601"
    ),

    PrimitiveMetadata(
      primitive = Primitives.Decimal,
      examples = Seq("10.12", "0.00", "-10.12"),
      description = "Commonly used to represent things like currency values. Maps to a BigDecimal in most languages."
    ),

    PrimitiveMetadata(
      primitive = Primitives.Double,
      examples = Seq("10.12", "0.00", "-10.12"),
      description = "double precision IEEE 754 floating-point number"
    ),

    PrimitiveMetadata(
      primitive = Primitives.Integer,
      examples = Seq("10", "0", "-10"),
      description = "32-bit signed integer"
    ),

    PrimitiveMetadata(
      primitive = Primitives.Long,
      examples = Seq("10", "0", "-10"),
      description = "64-bit signed integer"
    ),

    PrimitiveMetadata(
      primitive = Primitives.String,
      examples = Seq("This is a fox."),
      description = "unicode character sequence"
    ),

    PrimitiveMetadata(
      primitive = Primitives.Unit,
      examples = Seq.empty,
      description = "Internal type used to represent things like an HTTP NoContent response. Maps to void in Java, Unit in Scala, nil in ruby, etc."
    ),

    PrimitiveMetadata(
      primitive = Primitives.Uuid,
      examples = Seq("5ecf6502-e532-4738-aad5-7ac9701251dd"),
      description = "String representation of a universally unique identifier"
    )
  
  )

}
