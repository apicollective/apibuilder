package core

sealed abstract class Datatype(val name: String, val example: String, val description: String)

object Datatype {

  case object BooleanType extends Datatype(name = "boolean",
    example = "'true' or 'false'",
    description = "Represents a boolean value")

  case object DecimalType extends Datatype(name = "decimal",
    example = "10.12",
    description = "Commonly used to represent things like currency values. Maps to a BigDecimal in most languages.")

  case object IntegerType extends Datatype(name = "integer",
    example = "10",
    description = "32-bit signed integer")

  case object DoubleType extends Datatype(name = "double",
    example = "10.12",
    description = "double precision (64-bit) IEEE 754 floating-point number")

  case object LongType extends Datatype(name = "long",
    example = "10",
    description = "64-bit signed integer")

  case object StringType extends Datatype(name = "string",
    example = "This is a fox.",
    description = "unicode character sequence")

  case object MapType extends Datatype(name = "map",
    example = """{ "foo": "bar" }""",
    description = "A javascript object. The keys must be strings per JSON object spec. Apidoc requires the values to also be strings - while debatable, this encourages use of real models in APIs vs. maps, keeping use of maps to simpler use cases. The choice of string for value enables JSON serialization in all languages for all values of Maps - i.e. we can guarantee nice client interfaces. In typed languages (e.g. Scala), equivalent to Map[String, String]")

  case object DateIso8601Type extends Datatype(name = "date-iso8601",
    example = "2014-04-29",
    description = "Date format in ISO 8601")

  case object DateTimeIso8601Type extends Datatype(name = "date-time-iso8601",
    example = "2014-04-29T11:56:52Z",
    description = "Date time format in ISO 8601")

  case object UuidType extends Datatype(name = "uuid",
    example = "5ecf6502-e532-4738-aad5-7ac9701251dd",
    description = "String representation of a universally unique identifier (UUID)")

  case object UnitType extends Datatype(name = "unit",
    example = "N/A",
    description = "Internal type used to represent things like an HTTP NoContent response. Maps to void in Java, Unit in Scala, nil in ruby, etc.")

  val All: Seq[Datatype] = Seq(BooleanType, DecimalType, DoubleType, IntegerType, LongType, StringType, MapType, UuidType, DateIso8601Type, DateTimeIso8601Type)

  val QueryParameterTypes = All.filter(_ != MapType)

  def findByName(name: String): Option[Datatype] = {
    // TODO: This is weird. If we include UnitType in All - it ends up
    // being a NPE in the all loop. For now pull out unit explicitly
    if (name == UnitType.name) {
      Some(UnitType)
    } else {
      All.find { dt => dt.name == name }
    }
  }

  def forceByName(name: String) = findByName(name).getOrElse(sys.error("Invalid Datatype " + name))
}
