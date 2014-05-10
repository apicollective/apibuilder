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

  lazy val models = ModelResolver.build(internal.models)
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
    val model = models.find { _.plural == internal.resourceName }.getOrElse {
      sys.error("Could not find model for operation: " + internal)
    }

    val pathParameters = internal.namedParameters.map { paramName =>
      model.fields.find { _.name == paramName }.getOrElse {
        sys.error(s"Could not find operation path parameter with name[${paramName}] for model[${model.name}]")
      }
    }

    Operation(resourceName = internal.resourceName,
              method = internal.method.getOrElse { sys.error("Missing method") },
              path = internal.path,
              description = internal.description,
              parameters = pathParameters ++ internal.parameters.map { Field(models, _, None, Seq.empty) },
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

// TODO: Rename resource => modelPlural
case class Reference(resource: String, field: String) {

  lazy val label = s"$resource.$field"

}

case class Response(code: Int,
                    datatype: String,
                    multiple: Boolean = false)

object Model {

  def apply(models: Seq[Model], im: InternalModel): Model = {
    Model(name = im.name,
          plural = im.plural,
          description = im.description,
          fields = ModelResolver.buildFields(models, im))
  }

}

object Response {

  def apply(ir: InternalResponse): Response = {
    Response(code = ir.code.toInt,
             datatype = ir.datatype.get,
             multiple = ir.multiple)
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

  case object DateTimeIso8601 extends Datatype(name = "date-time-iso8601",
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

  def findByModelPluralAndFieldName(models: Seq[Model], modelPlural: String, fieldName: String): Option[Field] = {
    models.find { m => m.plural == modelPlural }.flatMap { _.fields.find { f => f.name == fieldName } }
  }

  def apply(models: Seq[Model], internal: InternalField, modelPlural: Option[String], fields: Seq[Field]): Field = {
    val datatype = internal.datatype match {
      case Some(name: String) => {
        Datatype.findByName(name).getOrElse {
          sys.error(s"Invalid datatype[${name}]")
        }
      }

      case None => {
        val ref = internal.references.getOrElse {
          sys.error("No datatype nor reference for field: " + internal)
        }

        val field = if (modelPlural == ref.modelPlural) {
          fields.find { _.name == ref.fieldName.get }.getOrElse {
            sys.error(s"Reference[${ref.label}] does not exist")
          }
        } else {
          Field.findByModelPluralAndFieldName(models, ref.modelPlural.get, ref.fieldName.get).getOrElse {
            sys.error(s"Reference[${ref.label}] does not exist")
          }
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
    Reference(internal.modelPlural.get, internal.fieldName.get)
  }

}
