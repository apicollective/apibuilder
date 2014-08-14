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

  lazy val enums: Seq[Enum] = internal.enums.map { Enum(_) }.sortBy(_.name.toLowerCase)
  lazy val models: Seq[Model] = internal.models.map { Model(this, _) }.sortBy(_.name.toLowerCase)
  lazy val resources: Seq[Resource] = internal.resources.map { Resource(enums, models, _) }.sorted
  lazy val baseUrl: Option[String] = internal.baseUrl
  lazy val name: String = internal.name.getOrElse { sys.error("Missing name") }
  lazy val description: Option[String] = internal.description

  /**
   * Returns a lits of Models that are not mapped to a Resource
   */
  def modelsWithoutResources(): Seq[Model] = {
    val modelNames = resources.map(_.model.name).toSet
    models.filter { m => !modelNames.contains(m.name) }
  }

  def model(name: String): Option[Model] = {
    models.find(_.name == name)
  }

}

case class Enum(
  name: String,
  description: Option[String],
  values: Seq[EnumValue]
) {
  require(Text.isValidName(name), s"Enum name[$name] is invalid - can only contain alphanumerics and underscores and must start with a letter")
}

case class EnumValue(
  name: String,
  description: Option[String]
) {
  require(Text.isValidName(name), s"Enum value[$name] is invalid - can only contain alphanumerics and underscores and must start with a letter")
}

case class Model(name: String,
                 plural: String,
                 description: Option[String],
                 fields: Seq[Field]) extends Ordered[Model] {
  require(Text.isValidName(name), s"Model name[$name] is invalid - can only contain alphanumerics and underscores and must start with a letter")

  def compare(that: Model): Int = {
    name.toLowerCase.compare(that.name.toLowerCase)
  }

}

case class Resource(model: Model,
                    path: String,
                    operations: Seq[Operation]) extends Ordered[Resource] {

  def compare(that: Resource): Int = {
    model.plural.toLowerCase.compare(that.model.plural.toLowerCase)
  }

}


case class Operation(model: Model,
                     method: String,
                     path: String,
                     description: Option[String],
                     body: Option[Model],
                     parameters: Seq[Parameter],
                     responses: Seq[Response]) {

  lazy val label = "%s %s".format(method, path)

}

object Resource {

  def apply(enums: Seq[Enum], models: Seq[Model], internal: InternalResource): Resource = {
    val model = models.find { _.name == internal.modelName.get }.getOrElse {
      sys.error(s"Could not find model for resource[${internal.modelName.getOrElse("")}]")
    }
    Resource(model = model,
             path = internal.path,
             operations = internal.operations.map(op => Operation(enums, models, model, op)))
  }

}

object Operation {

  def apply(enums: Seq[Enum], models: Seq[Model], model: Model, internal: InternalOperation): Operation = {
    val method = internal.method.getOrElse { sys.error("Missing method") }
    val location = if (!internal.body.isEmpty || method == "GET") { ParameterLocation.Query } else { ParameterLocation.Form }
    val internalParams = internal.parameters.map { p =>
      if (internal.namedPathParameters.contains(p.name.get)) {
        Parameter(enums, models, p, ParameterLocation.Path)
      } else {
        Parameter(enums, models, p, location)
      }
     }
    val internalParamNames: Set[String] = internalParams.map(_.name).toSet

    // Capture any path parameters that were not explicitly annotated
    val pathParameters = internal.namedPathParameters.filter { name => !internalParamNames.contains(name) }.map { Parameter.fromPath(model, _) }

    val bodyModel: Option[Model] = internal.body match {
      case None => None
      case Some(modelName) => {
        Some(
          models.find(_.name == modelName).getOrElse {
            sys.error(s"Operation specifies body[$modelName] which references an undefined model")
          }
        )
      }
    }

    Operation(model = model,
              method = method,
              path = internal.path,
              description = internal.description,
              body = bodyModel,
              parameters = pathParameters ++ internalParams,
              responses = internal.responses.map { Response(_) })
  }

}

case class Field(name: String,
                 fieldtype: FieldType,
                 description: Option[String] = None,
                 required: Boolean = true,
                 multiple: Boolean = false,
                 default: Option[String] = None,
                 example: Option[String] = None,
                 minimum: Option[Long] = None,
                 maximum: Option[Long] = None)

sealed trait FieldType
case class PrimitiveFieldType(datatype: Datatype) extends FieldType
case class ModelFieldType(modelName: String) extends FieldType
case class EnumFieldType(enum: Enum) extends FieldType

sealed trait ParameterType
case class PrimitiveParameterType(datatype: Datatype) extends ParameterType
case class ModelParameterType(model: Model) extends ParameterType
case class EnumParameterType(enum: Enum) extends ParameterType

case class Parameter(name: String,
                     paramtype: ParameterType,
                     location: ParameterLocation,
                     description: Option[String] = None,
                     required: Boolean = true,
                     multiple: Boolean = false,
                     default: Option[String] = None,
                     example: Option[String] = None,
                     minimum: Option[Long] = None,
                     maximum: Option[Long] = None)


case class Response(code: Int,
                    datatype: String,
                    multiple: Boolean = false)

object Enum {

  def apply(ie: InternalEnum): Enum = {
    Enum(
      name = ie.name,
      description = ie.description,
      values = ie.values.map { iv => EnumValue(iv.name.get, iv.description) }
    )
  }

}

object Model {

  def apply(sd: ServiceDescription, im: InternalModel): Model = {
    Model(name = im.name,
          plural = im.plural,
          description = im.description,
          fields = im.fields.map { Field(sd.enums, im, _) })
  }

}

object Response {

  def apply(ir: InternalResponse): Response = {
    val dt = ir.datatype.getOrElse {
      sys.error("No datatype for response: " + ir)
    }
    Response(code = ir.code.toInt,
             datatype = dt,
             multiple = ir.multiple)
  }

}

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
                                       description = "A javascript object. The keys must be strings per JSON object spec. Apidoc requires the values to also be strings - while debatable, this encourages use of real models in APIs vs. maps, keeping use of maps to simpler use cases. The choice of string for value as enables JSON serialization in all languages for all values of Maps - i.e. we can guarantee nice client interfaces. In typed languages (e.g. Scala), equivalent to Map[String, String]")

  case object DateTimeIso8601Type extends Datatype(name = "date-time-iso8601",
                                                   example = "2014-04-29T11:56:52Z",
                                                   description = "Date time format in ISO 8601")

  case object UuidType extends Datatype(name = "uuid",
                                        example = "5ecf6502-e532-4738-aad5-7ac9701251dd",
                                        description = "String representation of a universally unique identifier (UUID)")

  case object UnitType extends Datatype(name = "unit",
                                        example = "N/A",
                                        description = "Internal type used to represent things like an HTTP NoContent response. Maps to void in Java, Unit in Scala, nil in ruby, etc.")

  val All: Seq[Datatype] = Seq(BooleanType, DecimalType, DoubleType, IntegerType, LongType, StringType, MapType, UuidType, DateTimeIso8601Type)

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

}

sealed abstract class ParameterLocation(val name: String)

object ParameterLocation {

  case object Path extends ParameterLocation("path")
  case object Query extends ParameterLocation("query")
  case object Form extends ParameterLocation("form")

}

object Parameter {

  def fromPath(model: Model, name: String): Parameter = {
    val datatype = model.fields.find(_.name == name) match {
      case None => Datatype.StringType
      case Some(f: Field) => {
        f.fieldtype match {
          case ft: PrimitiveFieldType => ft.datatype
          case _ => Datatype.StringType
        }
      }
    }

    Parameter(name = name,
              paramtype = PrimitiveParameterType(datatype),
              location = ParameterLocation.Path,
              required = true)
  }

  def apply(enums: Seq[Enum], models: Seq[Model], internal: InternalParameter, location: ParameterLocation): Parameter = {
    val typeName = internal.paramtype.getOrElse {
      sys.error("Missing parameter type for: " + internal)
    }

    val paramtype = Datatype.findByName(typeName) match {
      case None => {
        enums.find(_.name == typeName) match {
          case Some(enum) => {
            internal.default.map { v => Field.assertValidDefault(Datatype.StringType, v) }
            EnumParameterType(enum)
          }

          case None => {
            assert(internal.default.isEmpty, "Can only have a default for a primitive datatype")
            ModelParameterType(models.find(_.name == typeName).getOrElse {
              sys.error(s"Param type[${typeName}] is invalid. Must be a valid primitive datatype or the name of a known model")
            })
          }
        }
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

  def apply(enums: Seq[Enum], im: InternalModel, internal: InternalField): Field = {
    val fieldTypeName = internal.fieldtype.getOrElse {
      sys.error("missing field type")
    }

    val fieldtype = Datatype.findByName(fieldTypeName) match {
      case Some(dt: Datatype) => {
        internal.default.map { v => assertValidDefault(dt, v) }
        PrimitiveFieldType(dt)
      }

      case None => {
        enums.find(_.name == fieldTypeName) match {
          case Some(e: Enum) => {
            internal.default.map { v => assertValidDefault(Datatype.StringType, v) }
            EnumFieldType(e)
          }
          case None => {
            require(internal.default.isEmpty, s"Cannot have a default for a field of type[$fieldTypeName]")
            ModelFieldType(fieldTypeName)
          }
        }
      }
    }

    Field(name = internal.name.get,
          fieldtype = fieldtype,
          description = internal.description,
          required = internal.required,
          multiple = internal.multiple,
          default = internal.default,
          minimum = internal.minimum.map(_.toLong),
          maximum = internal.maximum.map(_.toLong),
          example = internal.example)
  }

  private val BooleanValues = Seq("true", "false")

  /**
    * Returns true if the specified value is valid for the given
    * datatype. False otherwise.
    */
  def isValid(datatype: Datatype, value: String): Boolean = {
    try {
      assertValidDefault(datatype: Datatype, value: String)
      true
    } catch {
      case e: Throwable => {
        false
      }
    }
  }

  def assertValidDefault(datatype: Datatype, value: String) {
    datatype match {
      case Datatype.BooleanType => {
        if (!BooleanValues.contains(value)) {
          sys.error(s"Invalid default[${value}] for boolean. Must be one of: ${BooleanValues.mkString(" ")}")
        }
      }

      case Datatype.MapType => {
        Json.parse(value).asOpt[JsObject] match {
          case None => {
            sys.error(s"Invalid default[${value}] for type object. Must be a valid JSON Object")
          }
          case Some(o) => {}
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

      case Datatype.DoubleType => value.toDouble

    }
  }

}

