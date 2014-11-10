package core

import java.util.UUID
import org.joda.time.format.ISODateTimeFormat

case class TypeValidatorEnums(name: String, values: Seq[String])

object TypeValidator {
  private[core] val BooleanValues = Seq("true", "false")
}

case class TypeValidator(
  enums: Seq[TypeValidatorEnums] = Seq.empty
) {

  private[this] val dateTimeISOParser = ISODateTimeFormat.dateTimeParser()

  def assertValidDefault(t: Type, value: String) {
    validate(t, value) match {
      case None => ()
      case Some(msg) => sys.error(msg)
    }
  }

  def validateTypeInstance(
    typeInstance: TypeInstance,
    value: String,
    errorPrefix: Option[String] = None
  ): Option[String] = {
    typeInstance.container match {
      case TypeContainer.Singleton => {
        validate(typeInstance.`type`, value, errorPrefix)
      }
      case TypeContainer.List => {
        sys.error("TypeContainer.List: " + value)
        validate(typeInstance.`type`, value, errorPrefix)
      }
      case TypeContainer.Map => {
        sys.error("TypeContainer.Map: " + value)
      }
    }
  }

  def validate(
    t: Type,
    value: String,
    errorPrefix: Option[String] = None
  ): Option[String] = {
    t match {

      case Type.Enum(name) => {
        enums.find(_.name == name) match {
          case None => Some(s"could not find enum named[$name]")
          case Some(enum) => {
            enum.values.find(_ == value) match {
              case None => {
                val msg = s"default[$value] is not a valid value for enum[$name]. Valid values are: " + enum.values.mkString(", ")
                errorPrefix match {
                  case None => Some(msg)
                  case Some(prefix) => Some(s"$prefix $msg")
                }
              }
              case Some(_) => None
            }
          }
        }
      }
      
      case Type.Model(name) => {
        Some(s"default[$value] is not valid for model[$name]. apidoc does not support default values for models")
      }

      case Type.Primitive(Primitives.Boolean) => {
        if (TypeValidator.BooleanValues.contains(value)) {
          None
        } else {
          Some(s"Value[$value] is not a valid boolean. Must be one of: ${TypeValidator.BooleanValues.mkString(", ")}")
        }
      }

      case Type.Primitive(Primitives.Double) => {
        try {
          value.toDouble
          None
        } catch {
          case _: Throwable => Some(s"Value[$value] is not a valid double")
        }
      }

      case Type.Primitive(Primitives.Integer) => {
        try {
          value.toInt
          None
        } catch {
          case _: Throwable => Some(s"Value[$value] is not a valid integer")
        }
      }

      case Type.Primitive(Primitives.Long) => {
        try {
          value.toLong
          None
        } catch {
          case _: Throwable => Some(s"Value[$value] is not a valid long")
        }
      }

      case Type.Primitive(Primitives.Decimal) => {
        try {
          BigDecimal(value)
          None
        } catch {
          case _: Throwable => Some(s"Value[$value] is not a valid decimal")
        }
      }

      case Type.Primitive(Primitives.Unit) => {
        if (value == "") {
          None
        } else {
          Some(s"Value[$value] is not a valid unit type - must be the empty string")
        }
      }

      case Type.Primitive(Primitives.Uuid) => {
        try {
          UUID.fromString(value)
          None
        } catch {
          case _: Throwable => Some(s"Value[$value] is not a valid uuid")
        }
      }

      case Type.Primitive(Primitives.DateIso8601) => {
        try {
          dateTimeISOParser.parseDateTime(s"${value}T00:00:00Z")
          None
        } catch {
          case _: Throwable => Some(s"Value[$value] is not a valid date-iso8601")
        }
      }

      case Type.Primitive(Primitives.DateTimeIso8601) => {
        try {
          dateTimeISOParser.parseDateTime(value)
          None
        } catch {
          case _: Throwable => Some(s"Value[$value] is not a valid date-time-iso8601")
        }
      }

      case Type.Primitive(Primitives.String) => {
        None
      }
    }
  }

}
