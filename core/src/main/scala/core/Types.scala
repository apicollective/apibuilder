package core

import com.gilt.apidocgenerator.models._

sealed trait Primitives

object Primitives {

  case object Boolean extends Primitives { override def toString = "boolean" }
  case object Decimal extends Primitives { override def toString = "decimal" }
  case object Integer extends Primitives { override def toString = "integer" }
  case object Double extends Primitives { override def toString = "double" }
  case object Long extends Primitives { override def toString = "long" }
  case object String extends Primitives { override def toString = "string" }
  case object DateIso8601 extends Primitives { override def toString = "date-iso8601" }
  case object DateTimeIso8601 extends Primitives { override def toString = "date-time-iso8601" }
  case object Uuid extends Primitives { override def toString = "uuid" }
  case object Unit extends Primitives { override def toString = "unit" }

  val All = Seq(Boolean, Decimal, Integer, Double, Long, String, DateIso8601, DateTimeIso8601, Uuid, Unit)

  val ValidInPath = All.filter(_ != Unit)

  def validInPath(name: String): Boolean = {
    ValidInPath.find(_.toString == name) match {
      case None => false
      case Some(_) => true
    }
  }

  def apply(value: String): Option[Primitives] = {
    All.find(_.toString == value.toLowerCase.trim)
  }

}

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
      primitive = Primitives.Decimal,
      examples = Seq("10.12"),
      description = "Commonly used to represent things like currency values. Maps to a BigDecimal in most languages."
    ),

    PrimitiveMetadata(
      primitive = Primitives.Integer,
      examples = Seq("10"),
      description = "32-bit signed integer"
    ),

    PrimitiveMetadata(
      primitive = Primitives.Double,
      examples = Seq("10.12"),
      description = "double precision IEEE 754 floating-point number"
    ),

    PrimitiveMetadata(
      primitive = Primitives.Long,
      examples = Seq("10"),
      description = "64-bit signed integer"
    ),

    PrimitiveMetadata(
      primitive = Primitives.String,
      examples = Seq("This is a fox."),
      description = "unicode character sequence"
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
      primitive = Primitives.Uuid,
      examples = Seq("5ecf6502-e532-4738-aad5-7ac9701251dd"),
      description = "String representation of a universally unique identifier"
    ),

    PrimitiveMetadata(
      primitive = Primitives.Unit,
      examples = Seq.empty,
      description = "Internal type used to represent things like an HTTP NoContent response. Maps to void in Java, Unit in Scala, nil in ruby, etc."
    )
  
  )

}
