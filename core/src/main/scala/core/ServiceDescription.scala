package core

import play.api.libs.json._
import java.util.UUID
import org.joda.time.format.ISODateTimeFormat

object ServiceDescription {

  def apply(apiJson: String): ServiceDescription = {
    val jsValue = Json.parse(apiJson)
    ServiceDescription(jsValue)
  }

  def apply(json: JsValue): ServiceDescription = {
    val internal = InternalServiceDescription(json)
    ServiceDescription(internal)
  }

}

case class ServiceDescription(internal: InternalServiceDescription) {

  lazy val models = internal.models.map { Model(_) }
  lazy val operations = internal.operations.map { Operation(models, _) }
  lazy val baseUrl = internal.baseUrl.getOrElse { sys.error("Missing base_url") }
  lazy val name = internal.name.getOrElse { sys.error("Missing name") }
  lazy val basePath = internal.basePath
  lazy val description = internal.description

}

case class Model(name: String,
                 plural: String,
                 description: Option[String],
                 fields: Seq[Field])

case class Operation(resourceName: String,
                     method: String,
                     path: Option[String],
                     description: Option[String],
                     parameters: Seq[Field],
                     responses: Seq[Response])

object Operation {

  def apply(models: Seq[Model], internal: InternalOperation): Operation = {
    Operation(resourceName = internal.resourceName,
              method = internal.method.getOrElse { sys.error("Missing method") },
              path = internal.path,
              description = internal.description,
              parameters = internal.parameters.map { Field(models, _) },
              responses = internal.responses.map { Response(_) })
  }

}

case class Field(name: String,
                 datatype: Datatype,
                 description: Option[String] = None,
                 required: Boolean = true,
                 multiple: Boolean = false,
                 references: Option[Reference] = None,
                 default: Option[String] = None,
                 example: Option[String] = None,
                 minimum: Option[Long] = None,
                 maximum: Option[Long] = None)

case class Reference(resource: String, field: String) {

  lazy val label = s"$resource.$field"

}

// TODO: Rename resource to datatype
case class Response(code: Int,
                    resource: String,
                    multiple: Boolean = false)

object Model {

  def apply(ir: InternalModel): Model = {
    Model(name = ir.name,
          plural = ir.plural,
          description = ir.description,
          fields = ir.fields.map { Field(Seq.empty, _) }) // TODO: Reference lookups
  }

}

object Response {

  def apply(ir: InternalResponse): Response = {
    val wd = WrappedDatatype(ir.datatype.get)
    Response(code = ir.code.toInt,
             resource = wd.datatype.name,
             multiple = wd.multiple)
  }

}

case class WrappedDatatype(datatype: Datatype, multiple: Boolean)

object WrappedDatatype {

  private val ArrayRx = """^\[(.+)\]$""".r

  def apply(value: String): WrappedDatatype = {
    // TODO: Parse ir.datatype properly
    //val multiple = ArrayRx.matches(ir.datatype)
    val datatype = Datatype.findByName(value).getOrElse {
      sys.error(s"Invalid datatype[${value}]")
    }
    WrappedDatatype(datatype = datatype, multiple = false)
  }

}

sealed abstract class Datatype(val name: String, val example: Option[String] = None, val description: Option[String] = None) {

  /**
   * Returns true if the string can be converted into an instance of this
   * datatype. False otherwise.
   */
  // TODO: def isValid(value: String): Boolean = validate(value).isEmpty

  /**
   * If the provided value is valid for this datatype - returns None.
   * Otherwise, returns a validation message.
   */
    // TODO: def validate(value: String): Option[String]

}

object Datatype {

  case object Boolean extends Datatype("boolean")

  case object Decimal extends Datatype("decimal")
  case object Integer extends Datatype("integer")
  case object Long extends Datatype("long")
  case object String extends Datatype("string")

  case object Uuid extends Datatype(name = "uuid",
                                    example = Some("5ecf6502-e532-4738-aad5-7ac9701251dd"),
                                    description = Some("String representation of a universally unique identifier (UUID)"))

  case object DateTimeIso8601 extends Datatype(name = "date-time-iso-8601",
                                               example = Some("2014-04-29T11:56:52Z"),
                                               description = Some("Date time format in ISO 8601"))

  case object Unit extends Datatype("unit")
  // TODO: case object Object extends Datatype("objects")

  val All = Seq(Boolean, Decimal, Integer, Long, String, Uuid, DateTimeIso8601, Unit)

  def findByName(name: String): Option[Datatype] = {
    All.find { dt => dt.name == name }
  }

}

object Field {

  private def findByModelNameAndFieldName(models: Seq[Model], modelName: String, fieldName: String): Option[Field] = {
    models.find { m => m.name == modelName }.flatMap { _.fields.find { f => f.name == fieldName } }
  }

  def apply(models: Seq[Model], internal: InternalField): Field = {
    println("MODELS: " + models.map(_.name).mkString(" "))

    val datatype = internal.datatype match {
      case Some(t: String) => {
        Datatype.findByName(t).getOrElse {
          sys.error(s"Invalid datatype[${t}]")
        }
      }

      case None => {
        val ref = internal.references.getOrElse {
          sys.error("Missing datatype and/or reference for field: " + internal)
        }

        val field = Field.findByModelNameAndFieldName(models, ref.modelName.get, ref.fieldName.get).getOrElse {
          sys.error(s"Reference[${ref.label}] does not exist")
        }

        field.datatype
      }
    }

    internal.default.map { v => assertValidDefault(datatype, v) }

    Field(name = internal.name.get,
          datatype = datatype,
          description = internal.description,
          references = internal.references.map { Reference(_) },
          required = internal.required,
          multiple = internal.multiple,
          default = internal.default,
          minimum = internal.minimum.map(_.toLong),
          maximum = internal.maximum.map(_.toLong),
          example = internal.example)
  }


  private val BooleanValues = Seq("true", "false")

  private def assertValidDefault(datatype: Datatype, value: String) {
    datatype match {
      case Datatype.Boolean => {
        if (!BooleanValues.contains(value)) {
          sys.error(s"Invalid value[${value}] for boolean. Must be one of: ${BooleanValues.mkString(" ")}")
        }
      }

      case Datatype.Integer => {
        value.toInt
      }

      case Datatype.Long => {
        value.toLong
      }

      case Datatype.Decimal => {
        BigDecimal(value)
      }

      case Datatype.Unit => {
        value == ""
      }

      case Datatype.Uuid => {
        UUID.fromString(value)
      }

      case Datatype.DateTimeIso8601 => {
        ISODateTimeFormat.basicDateTime.parseDateTime(value)
      }

      case Datatype.String => ()

    }
  }

}

object Reference {

  def apply(internal: InternalReference): Reference = {
    Reference(internal.modelName.get, internal.fieldName.get)
  }

}
