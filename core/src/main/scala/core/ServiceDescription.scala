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

  def operationsForModel(model: Model): Seq[Operation] = {
    operations.filter(_.model.name == model.name)
  }

}

case class Model(name: String,
                 plural: String,
                 description: Option[String],
                 fields: Seq[Field])

case class Operation(model: Model,
                     method: String,
                     path: String,
                     description: Option[String],
                     parameters: Seq[Parameter],
                     responses: Seq[Response]) {

  lazy val label = "%s %s".format(method, path)

}

object Operation {

  def apply(models: Seq[Model], internal: InternalOperation): Operation = {
    val model = models.find { _.plural == internal.resourceName }.getOrElse {
      sys.error("Could not find model for operation: " + internal)
    }

    val pathParameters = internal.namedParameters.map { paramName =>
      model.fields.find { _.name == paramName }.map( f => Parameter(f, ParameterLocation.Path) ).getOrElse {
        sys.error(s"Could not find operation path parameter with name[${paramName}] for model[${model.name}]")
      }
    }

    val method = internal.method.getOrElse { sys.error("Missing method") }
    val location = if (method == "GET") { ParameterLocation.Query } else { ParameterLocation.Form }

    Operation(model = model,
              method = method,
              path = internal.path,
              description = internal.description,
              parameters = pathParameters ++ internal.parameters.map { Parameter(models, _, location) },
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

sealed trait ParameterType
case class PrimitiveParameterType(datatype: Datatype) extends ParameterType
case class ModelParameterType(model: Model) extends ParameterType

case class Parameter(name: String,
                     paramtype: ParameterType,
                     location: ParameterLocation,
                     description: Option[String] = None,
                     required: Boolean = true,
                     multiple: Boolean = false,
                     references: Option[Reference] = None,
                     default: Option[String] = None,
                     example: Option[String] = None,
                     minimum: Option[Long] = None,
                     maximum: Option[Long] = None)

case class Reference(modelPlural: String, fieldName: String) {

  lazy val label = s"$modelPlural.$fieldName"

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
    val dt = ir.datatype.getOrElse {
      sys.error("No datatype for respones: " + ir)
    }
    Response(code = ir.code.toInt,
             datatype = dt,
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

  case object BooleanType extends Datatype("boolean")
  case object DecimalType extends Datatype("decimal")
  case object IntegerType extends Datatype("integer")
  case object LongType extends Datatype("long")
  case object StringType extends Datatype("string")

  case object DateTimeIso8601Type extends Datatype(name = "date-time-iso8601",
                                               example = Some("2014-04-29T11:56:52Z"),
                                               description = Some("Date time format in ISO 8601"))

  case object UuidType extends Datatype(name = "uuid",
                                        example = Some("5ecf6502-e532-4738-aad5-7ac9701251dd"),
                                        description = Some("String representation of a universally unique identifier (UUID)"))

  case object UnitType extends Datatype("unit")

  // TODO: case object ObjectType extends Datatype("objects")

  val All: Seq[Datatype] = Seq(BooleanType, DecimalType, IntegerType, LongType, StringType, UuidType, DateTimeIso8601Type)

  def findByName(name: String): Option[Datatype] = {
    // TODO: This is weird. If we include UnitType in All - it ends up
    // being a NPE in the all loop. For now pull out unit explicitly
    if (name == UnitType.name) {
      Some(UnitType)
    } else {
      All.find { dt => dt.name == name }
    }
  }

}

sealed abstract class ParameterLocation(val name: String)

object ParameterLocation {

  case object Path extends ParameterLocation("path")
  case object Query extends ParameterLocation("query")
  case object Form extends ParameterLocation("form")

}

object Parameter {

  def apply(field: Field, location: ParameterLocation): Parameter = {
    Parameter(name = field.name,
              paramtype = PrimitiveParameterType(field.datatype),
              location = location,
              description = field.description,
              required = true)
  }

  def apply(models: Seq[Model], internal: InternalParameter, location: ParameterLocation): Parameter = {
    val typeName = internal.paramtype.getOrElse {
      sys.error("Missing parameter type for: " + internal)
    }

    val paramtype = Datatype.findByName(typeName) match {
      case None => {
        assert(internal.default.isEmpty, "Can only have a default for a primitive datatype")
        ModelParameterType(models.find(_.name == typeName).getOrElse {
          sys.error(s"Param type[${typeName}] is invalid. Must be a valid primitive datatype or the name of a known model")
        })
      }

      case Some(dt: Datatype) => {
        internal.default.map { v => Field.assertValidDefault(dt, v) }
        PrimitiveParameterType(dt)
      }
    }

    Parameter(name = internal.name.get,
              paramtype = paramtype,
              location = location,
              description = internal.description,
              required = internal.required,
              multiple = internal.multiple,
              default = internal.default,
              minimum = internal.minimum.map(_.toLong),
              maximum = internal.maximum.map(_.toLong),
              example = internal.example)
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

  def assertValidDefault(datatype: Datatype, value: String) {
    datatype match {
      case Datatype.BooleanType => {
        if (!BooleanValues.contains(value)) {
          sys.error(s"Invalid value[${value}] for boolean. Must be one of: ${BooleanValues.mkString(" ")}")
        }
      }

      case Datatype.IntegerType => {
        value.toInt
      }

      case Datatype.LongType => {
        value.toLong
      }

      case Datatype.DecimalType => {
        BigDecimal(value)
      }

      case Datatype.UnitType => {
        value == ""
      }

      case Datatype.UuidType => {
        UUID.fromString(value)
      }

      case Datatype.DateTimeIso8601Type => {
        ISODateTimeFormat.basicDateTime.parseDateTime(value)
      }

      case Datatype.StringType => ()

    }
  }

}

object Reference {

  def apply(internal: InternalReference): Reference = {
    Reference(internal.modelPlural.get, internal.fieldName.get)
  }

}
