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

  lazy val models: Seq[Model] = ModelResolver.build(internal.models).sorted
  lazy val resources: Seq[Resource] = internal.resources.map { Resource(models, _) }.sorted
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
                     parameters: Seq[Parameter],
                     responses: Seq[Response]) {

  lazy val label = "%s %s".format(method, path)

  lazy val pathParameters = parameters.filter { _.location == ParameterLocation.Path }

}

object Resource {

  def apply(models: Seq[Model], internal: InternalResource): Resource = {
    val model = models.find { _.name == internal.modelName.get }.getOrElse {
      sys.error(s"Could not find model for resource[${internal.modelName}]")
    }
    Resource(model = model,
             path = internal.path,
             operations = internal.operations.map(op => Operation(models, model, op)))
  }

}

object Operation {

  def apply(models: Seq[Model], model: Model, internal: InternalOperation): Operation = {
    val method = internal.method.getOrElse { sys.error("Missing method") }
    val location = if (method == "GET") { ParameterLocation.Query } else { ParameterLocation.Form }
    val internalParams = internal.parameters.map { p =>
      if (internal.namedParameters.contains(p.name.get)) {
        Parameter(models, p, ParameterLocation.Path)
      } else {
        Parameter(models, p, location)
      }
     }
    val internalParamNames: Set[String] = internalParams.map(_.name).toSet

    // Capture any path parameters that were not explicitly annotated
    val pathParameters = internal.namedParameters.filter { name => !internalParamNames.contains(name) }.map { Parameter.fromPath(model, _) }

    Operation(model = model,
              method = method,
              path = internal.path,
              description = internal.description,
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
case class ModelFieldType(model: Model) extends FieldType
case class EnumerationFieldType(values: Seq[String]) extends FieldType

sealed trait ParameterType
case class PrimitiveParameterType(datatype: Datatype) extends ParameterType
case class ModelParameterType(model: Model) extends ParameterType

object PrimitiveParameterType {

  def apply(field: Field): PrimitiveParameterType = {
    field.fieldtype match {

      case pft: PrimitiveFieldType => {
        PrimitiveParameterType(pft.datatype)
      }

      case mft: ModelFieldType => {
        sys.error("Cannot convert model fieldtype[%s] to parameter type".format(field.fieldtype))
      }
    }
  }
}

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

sealed abstract class Datatype(val name: String, val example: String, val description: String) {

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

  case object DateTimeIso8601Type extends Datatype(name = "date-time-iso8601",
                                                   example = "2014-04-29T11:56:52Z",
                                                   description = "Date time format in ISO 8601")

  case object UuidType extends Datatype(name = "uuid",
                                        example = "5ecf6502-e532-4738-aad5-7ac9701251dd",
                                        description = "String representation of a universally unique identifier (UUID)")

  case object UnitType extends Datatype(name = "unit",
                                        example = "N/A",
                                        description = "Internal type used to represent things like an HTTP NoContent response. Maps to void in Java, Unit in Scala, nil in ruby, etc.")

  val All: Seq[Datatype] = Seq(BooleanType, DecimalType, DoubleType, IntegerType, LongType, StringType, UuidType, DateTimeIso8601Type)

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
    val fieldtype = internal.fieldtype match {
      case Some(nft: InternalNamedFieldType) => {
        Datatype.findByName(nft.name) match {
          case Some(dt: Datatype) => {
            internal.default.map { v => assertValidDefault(dt, v) }
            PrimitiveFieldType(dt)
          }

          case None => {
            require(internal.default.isEmpty, s"Cannot have a default for a field of type[${nft.name}]")

            val model = models.find { m => m.name == nft.name }.getOrElse {
              sys.error(s"Invalid field type[${nft.name}]. Must be a valid primitive datatype or the name of a known model")
            }
            ModelFieldType(model)
          }
        }
      }

      case None => {
        sys.error("missing field type")
      }
    }

    val finalFieldType = if (internal.values.isEmpty) {
      fieldtype
    } else {
      assert(fieldtype == PrimitiveFieldType(Datatype.StringType), "Field type must be string for enumerations")
      internal.values.foreach { value =>
        val errors = Text.validateName(value)
        assert(errors.isEmpty, s"Field[${internal.name.get}] has an invalid value[${value}]: " + errors.mkString(" "))
      }
      EnumerationFieldType(internal.values)
    }

    Field(name = internal.name.get,
          fieldtype = finalFieldType,
          description = internal.description,
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

      case Datatype.DoubleType => value.toDouble

    }
  }

}

