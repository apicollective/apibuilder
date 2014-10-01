package core

import codegenerator.models._
import play.api.libs.json._
import java.util.UUID
import org.joda.time.format.ISODateTimeFormat

object ServiceDescriptionBuilder {

  def apply(apiJson: String): ServiceDescription = {
    apply(apiJson, None)
  }

  def apply(apiJson: String, packageName: Option[String]): ServiceDescription = {
    val jsValue = Json.parse(apiJson)
    ServiceDescriptionBuilder(jsValue, packageName)
  }

  def apply(json: JsValue): ServiceDescription = {
    apply(json, None)
  }

  def apply(json: JsValue, packageName: Option[String]): ServiceDescription = {
    val internal = InternalServiceDescription(json)
    ServiceDescriptionBuilder(internal, packageName)
  }

  def apply(internal: InternalServiceDescription, packageName: Option[String] = None): ServiceDescription = {
    val enums = internal.enums.map(EnumBuilder(_)).sortBy(_.name.toLowerCase)
    val models = internal.models.map(ModelBuilder(enums, _)).sortBy(_.name.toLowerCase)
    val headers = internal.headers.map(HeaderBuilder(enums, _))
    val resources = internal.resources.map(ResourceBuilder(enums, models, _)).sortBy(_.model.plural.toLowerCase)
    ServiceDescription(
      enums,
      models,
      headers,
      resources,
      internal.baseUrl,
      internal.name.getOrElse(sys.error("Missing name")),
      packageName,
      internal.description
    )
  }
}

object ResourceBuilder {

  def apply(enums: Seq[Enum], models: Seq[Model], internal: InternalResource): Resource = {
    val model = models.find { _.name == internal.modelName.get }.getOrElse {
      sys.error(s"Could not find model for resource[${internal.modelName.getOrElse("")}]")
    }
    Resource(model = model,
             path = internal.path,
             operations = internal.operations.map(op => OperationBuilder(enums, models, model, op)))
  }

}

object OperationBuilder {

  def apply(enums: Seq[Enum], models: Seq[Model], model: Model, internal: InternalOperation): Operation = {
    val method = internal.method.getOrElse { sys.error("Missing method") }
    val location = if (!internal.body.isEmpty || method == "GET") { ParameterLocation.Query } else { ParameterLocation.Form }
    val internalParams = internal.parameters.map { p =>
      if (internal.namedPathParameters.contains(p.name.get)) {
        ParameterBuilder(enums, models, p, ParameterLocation.Path)
      } else {
        ParameterBuilder(enums, models, p, location)
      }
     }
    val internalParamNames: Set[String] = internalParams.map(_.name).toSet

    // Capture any path parameters that were not explicitly annotated
    val pathParameters = internal.namedPathParameters.filter { name => !internalParamNames.contains(name) }.map { ParameterBuilder.fromPath(model, _) }

    val body: Option[Type] = internal.body.map { ib =>
      Datatype.findByName(ib.name) match {
        case Some(dt) => Type(TypeKind.Primitive, dt.name, ib.multiple)
        case None => {
          models.find(_.name == ib.name) match {
            case Some(model) => Type(TypeKind.Model, model.name, ib.multiple)
            case None => {
              enums.find(_.name == ib.name) match {
                case Some(enum) => Type(TypeKind.Enum, enum.name, ib.multiple)
                case None => {
                  sys.error(s"Operation specifies body[$ib.name] which references an undefined datatype, model or enum")
                }
              }
            }
          }
        }
      }
    }

    Operation(model = model,
              method = method,
              path = internal.path,
              description = internal.description,
              body = body,
              parameters = pathParameters ++ internalParams,
              responses = internal.responses.map { ResponseBuilder(_) })
  }

}

object EnumBuilder {

  def apply(ie: InternalEnum): Enum = {
    Enum(
      name = ie.name,
      description = ie.description,
      values = ie.values.map { iv => EnumValue(iv.name.get, iv.description) }
    )
  }

}

object HeaderBuilder {

  def apply(enums: Seq[Enum], ih: InternalHeader): Header = {
    val (headertype, headervalue) = if (ih.headertype.get == Datatype.StringType.name) {
      HeaderType.String -> None
    } else {
      val enum = enums.find(_.name == ih.headertype.get).getOrElse {
        sys.error(s"Invalid header type[${ih.headertype.get}]")
      }
      HeaderType.Enum -> Some(enum.name)
    }

    Header(
      name = ih.name.get,
      headertype = headertype,
      headertypeValue = headervalue,
      multiple = ih.multiple,
      required = ih.required,
      description = ih.description,
      default = ih.default
    )
  }

}

object ModelBuilder {

  def apply(enums: Seq[Enum], im: InternalModel): Model = {
    Model(name = im.name,
          plural = im.plural,
          description = im.description,
          fields = im.fields.map { FieldBuilder(enums, im, _) })
  }

}

object ResponseBuilder {

  def apply(ir: InternalResponse): Response = {
    val dt = ir.datatype.getOrElse {
      sys.error("No datatype for response: " + ir)
    }
    Response(code = ir.code.toInt,
             datatype = dt,
             multiple = ir.multiple)
  }

}

//sealed abstract class Datatype(val name: String, val example: String, val description: String)
//
//object Datatype {
//
//  case object BooleanType extends Datatype(name = "boolean",
//                                           example = "'true' or 'false'",
//                                           description = "Represents a boolean value")
//
//  case object DecimalType extends Datatype(name = "decimal",
//                                           example = "10.12",
//                                           description = "Commonly used to represent things like currency values. Maps to a BigDecimal in most languages.")
//
//  case object IntegerType extends Datatype(name = "integer",
//                                           example = "10",
//                                           description = "32-bit signed integer")
//
//  case object DoubleType extends Datatype(name = "double",
//                                          example = "10.12",
//                                          description = "double precision (64-bit) IEEE 754 floating-point number")
//
//  case object LongType extends Datatype(name = "long",
//                                        example = "10",
//                                        description = "64-bit signed integer")
//
//  case object StringType extends Datatype(name = "string",
//                                          example = "This is a fox.",
//                                          description = "unicode character sequence")
//
//  case object MapType extends Datatype(name = "map",
//                                       example = """{ "foo": "bar" }""",
//                                       description = "A javascript object. The keys must be strings per JSON object spec. Apidoc requires the values to also be strings - while debatable, this encourages use of real models in APIs vs. maps, keeping use of maps to simpler use cases. The choice of string for value enables JSON serialization in all languages for all values of Maps - i.e. we can guarantee nice client interfaces. In typed languages (e.g. Scala), equivalent to Map[String, String]")
//
//  case object DateIso8601Type extends Datatype(name = "date-iso8601",
//                                               example = "2014-04-29",
//                                               description = "Date format in ISO 8601")
//
//  case object DateTimeIso8601Type extends Datatype(name = "date-time-iso8601",
//                                                   example = "2014-04-29T11:56:52Z",
//                                                   description = "Date time format in ISO 8601")
//
//  case object UuidType extends Datatype(name = "uuid",
//                                        example = "5ecf6502-e532-4738-aad5-7ac9701251dd",
//                                        description = "String representation of a universally unique identifier (UUID)")
//
//  case object UnitType extends Datatype(name = "unit",
//                                        example = "N/A",
//                                        description = "Internal type used to represent things like an HTTP NoContent response. Maps to void in Java, Unit in Scala, nil in ruby, etc.")
//
//  val All: Seq[Datatype] = Seq(BooleanType, DecimalType, DoubleType, IntegerType, LongType, StringType, MapType, UuidType, DateIso8601Type, DateTimeIso8601Type)
//
//  val QueryParameterTypes = All.filter(_ != MapType)
//
//  def findByName(name: String): Option[Datatype] = {
//    // TODO: This is weird. If we include UnitType in All - it ends up
//    // being a NPE in the all loop. For now pull out unit explicitly
//    if (name == UnitType.name) {
//      Some(UnitType)
//    } else {
//      All.find { dt => dt.name == name }
//    }
//  }
//
//}

//sealed abstract class ParameterLocation(val name: String)
//
//object ParameterLocation {
//
//  case object Path extends ParameterLocation("path")
//  case object Query extends ParameterLocation("query")
//  case object Form extends ParameterLocation("form")
//
//}

object ParameterBuilder {

  def fromPath(model: Model, name: String): Parameter = {
    val datatype = model.fields.find(_.name == name) match {
      case None => Datatype.StringType.name
      case Some(f: Field) => {
        f.fieldtype match {
          case Type(TypeKind.Primitive, name, _) => name
          case _ => Datatype.StringType.name
        }
      }
    }

    Parameter(name = name,
              paramtype = Type(TypeKind.Primitive, datatype, false),
              location = ParameterLocation.Path,
              required = true,
              multiple = false)
  }

  def apply(enums: Seq[Enum], models: Seq[Model], internal: InternalParameter, location: ParameterLocation): Parameter = {
    val typeName = internal.paramtype.getOrElse {
      sys.error("Missing parameter type for: " + internal)
    }

    val paramtype = Datatype.findByName(typeName) match {
      case None => {
        enums.find(_.name == typeName) match {
          case Some(enum) => {
            internal.default.map { v => FieldBuilder.assertValidDefault(Datatype.StringType, v) }
            Type(TypeKind.Enum, enum.name, false)
          }

          case None => {
            assert(internal.default.isEmpty, "Can only have a default for a primitive datatype")
            Type(TypeKind.Model, models.find(_.name == typeName).map(_.name).getOrElse {
              sys.error(s"Param type[${typeName}] is invalid. Must be a valid primitive datatype or the name of a known model")
            }, false)
          }
        }
      }

      case Some(dt: Datatype) => {
        internal.default.map { v => FieldBuilder.assertValidDefault(dt, v) }
        Type(TypeKind.Primitive, dt.name, false)
      }
    }

    Parameter(name = internal.name.get,
              paramtype = paramtype,
              location = location,
              description = internal.description,
              required = internal.required,
              multiple = internal.multiple,
              default = internal.default,
              minimum = internal.minimum,
              maximum = internal.maximum,
              example = internal.example)
  }

}

object FieldBuilder {

  def apply(enums: Seq[Enum], im: InternalModel, internal: InternalField): Field = {
    val fieldTypeName = internal.fieldtype.getOrElse {
      sys.error("missing field type")
    }

    val fieldtype = Datatype.findByName(fieldTypeName) match {
      case Some(dt: Datatype) => {
        internal.default.map { v => assertValidDefault(dt, v) }
        Type(TypeKind.Primitive, dt.name, false)
      }

      case None => {
        enums.find(_.name == fieldTypeName) match {
          case Some(e: Enum) => {
            internal.default.map { v => assertValidDefault(Datatype.StringType, v) }
            Type(TypeKind.Enum, e.name, false)
          }
          case None => {
            require(internal.default.isEmpty, s"Cannot have a default for a field of type[$fieldTypeName]")
            Type(TypeKind.Model, fieldTypeName, false)
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
          minimum = internal.minimum,
          maximum = internal.maximum,
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

  private[this] val dateTimeISOParser = ISODateTimeFormat.dateTimeParser()
  private[this] val dateTimeISOFormatter = ISODateTimeFormat.dateTime()

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
        dateTimeISOParser.parseDateTime(value)
      }

      case Datatype.DateIso8601Type => {
        dateTimeISOParser.parseDateTime(s"${value}T00:00:00Z")
      }

      case Datatype.StringType => ()

      case Datatype.DoubleType => value.toDouble

    }
  }

}

