package core.generator

import core._
import Text._

class AvroServiceDescription(sd: ServiceDescription) {

  val namespace = Text.safeName(sd.name).toLowerCase

  def models = sd.models.map(AvroModel(_))

}

case class AvroModel(name: String, fields: Seq[AvroField], namespace: Option[String] = None)

object AvroModel {
  def apply(model: Model): AvroModel = new AvroModel(model.name, model.fields.map(AvroField(_)))
}

case class AvroField(name: String, fieldtype: AvroType, default: Option[String] = None)

object AvroField {
  def apply(field: Field) = {
    val baseAvroType = determineType(field.fieldtype)
    val avroType =
      if (field.multiple) {
        AvroArrayType(baseAvroType)
      } else if (!field.required) {
        AvroUnionType(Seq(baseAvroType, AvroPrimitiveType.Null))
      } else {
        baseAvroType
      }

    new AvroField(field.name, avroType, field.default)
  }

  private def determineType(t: FieldType): AvroType = t match {
    case PrimitiveFieldType(d) => primativeType(d)
    case ModelFieldType(name) => new AvroModelType(name)
    case EnumerationFieldType(d, values) => AvroEnumType(values)
  }

  private def primativeType(d: Datatype): AvroType = {
    import Datatype._
    d match {
      case BooleanType => AvroPrimitiveType.Boolean
      case DecimalType => BIGDECIMAL
      case IntegerType => AvroPrimitiveType.Int
      case DoubleType => AvroPrimitiveType.Double
      case LongType => AvroPrimitiveType.Long
      case StringType => AvroPrimitiveType.String
      case MapType => AvroMapType(AvroPrimitiveType.String)
      case DateTimeIso8601Type => DATETIME
      case UuidType => UUID
      case UnitType => AvroPrimitiveType.Null
    }
  }

  val UUID = AvroFixedType("UUID", "java.util", 16)
  val BIGDECIMAL =
    AvroRecordType(new AvroModel(
      "BigDecimal",
      Seq(AvroField("bigInt", AvroPrimitiveType.Bytes), AvroField("scale", AvroPrimitiveType.Int)),
      Some("java.math"))
    )
  val DATETIME =
    AvroRecordType(new AvroModel(
      "DateTime",
      Seq(AvroField("timestamp", AvroPrimitiveType.Long), AvroField("timezone", AvroPrimitiveType.String)),
      Some("org.joda.time"))
    )
}

sealed abstract class AvroType

sealed abstract class AvroPrimitiveType(val name: String) extends AvroType
object AvroPrimitiveType {
  case object Null extends AvroPrimitiveType("null")
  case object Boolean extends AvroPrimitiveType("boolean")
  case object Int extends AvroPrimitiveType("int")
  case object Long extends AvroPrimitiveType("long")
  case object Float extends AvroPrimitiveType("float")
  case object Double extends AvroPrimitiveType("double")
  case object Bytes extends AvroPrimitiveType("bytes")
  case object String extends AvroPrimitiveType("string")
}

case class AvroModelType(modelName: String) extends AvroType
case class AvroRecordType(model: AvroModel) extends AvroType
case class AvroEnumType(symbols: Seq[String]) extends AvroType
case class AvroArrayType(itemType: AvroType) extends AvroType
case class AvroMapType(valueType: AvroType) extends AvroType
case class AvroUnionType(alternatives: Seq[AvroType]) extends AvroType
case class AvroFixedType(name: String, namespace: String, size: Integer) extends AvroType
