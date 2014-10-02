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
              responses = internal.responses.map { ResponseBuilder(enums, models, _) })
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

  def apply(enums: Seq[Enum], models: Seq[Model], internal: InternalResponse): Response = {
    val typeName = internal.datatype.getOrElse {
      sys.error("No datatype for response: " + internal)
    }
    val responseType = Datatype.findByName(typeName) match {
      case None => {
        enums.find(_.name == typeName) match {
          case Some(enum) => {
            Type(TypeKind.Enum, enum.name, internal.multiple)
          }

          case None => {
            Type(TypeKind.Model, models.find(_.name == typeName).map(_.name).getOrElse {
              sys.error(s"Param type[${typeName}] is invalid. Must be a valid primitive datatype or the name of a known model")
            }, internal.multiple)
          }
        }
      }

      case Some(dt: Datatype) => {
        Type(TypeKind.Primitive, dt.name, internal.multiple)
      }
    }

    Response(code = internal.code.toInt,
             datatype = responseType)
  }

}

object ParameterBuilder {

  def fromPath(model: Model, name: String): Parameter = {
    val datatype = model.fields.find(_.name == name) match {
      case None => Datatype.StringType.name
      case Some(f: Field) => {
        f.datatype match {
          case Type(TypeKind.Primitive, name, _) => name
          case _ => Datatype.StringType.name
        }
      }
    }

    Parameter(name = name,
              datatype = Type(TypeKind.Primitive, datatype, false),
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
            internal.default.map { v => FieldBuilder.assertValidDefault(Datatype.StringType, v) }
            Type(TypeKind.Enum, enum.name, internal.multiple)
          }

          case None => {
            assert(internal.default.isEmpty, "Can only have a default for a primitive datatype")
            Type(TypeKind.Model, models.find(_.name == typeName).map(_.name).getOrElse {
              sys.error(s"Param type[${typeName}] is invalid. Must be a valid primitive datatype or the name of a known model")
            }, internal.multiple)
          }
        }
      }

      case Some(dt: Datatype) => {
        internal.default.map { v => FieldBuilder.assertValidDefault(dt, v) }
        Type(TypeKind.Primitive, dt.name, internal.multiple)
      }
    }

    Parameter(name = internal.name.get,
              datatype = paramtype,
              location = location,
              description = internal.description,
              required = internal.required,
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
        Type(TypeKind.Primitive, dt.name, internal.multiple)
      }

      case None => {
        enums.find(_.name == fieldTypeName) match {
          case Some(e: Enum) => {
            internal.default.map { v => assertValidDefault(Datatype.StringType, v) }
            Type(TypeKind.Enum, e.name, internal.multiple)
          }
          case None => {
            require(internal.default.isEmpty, s"Cannot have a default for a field of type[$fieldTypeName]")
            Type(TypeKind.Model, fieldTypeName, internal.multiple)
          }
        }
      }
    }

    Field(name = internal.name.get,
          datatype = fieldtype,
          description = internal.description,
          required = internal.required,
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

