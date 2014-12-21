package lib

case class PrimitiveMetadata(
  primitive: Primitives,
  description: String,
  examples: Seq[String]
)

object PrimitiveMetadata {

  val BooleanValues = Seq("true", "false")

  val All = Seq(
    PrimitiveMetadata(
      primitive = Primitives.Boolean,
      examples = BooleanValues,
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
      primitive = Primitives.Object,
      examples = Seq("{}"),
      description = "Represents an arbitrary json object. In scala clients using play-json, maps to a play.api.libs.json.JsObject. In ruby clients, JSON is parsed into a hash"
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
