package core

import lib.Primitives
import com.gilt.apidocgenerator.models._
import play.api.libs.json._
import org.joda.time.format.ISODateTimeFormat

object ServiceDescriptionBuilder {

  def apply(apiJson: String): ServiceDescription = {
    apply(apiJson, None, None)
  }

  def apply(apiJson: String, packageName: Option[String], userAgent: Option[String]): ServiceDescription = {
    val jsValue = Json.parse(apiJson)
    ServiceDescriptionBuilder(jsValue, packageName, userAgent)
  }

  def apply(json: JsValue): ServiceDescription = {
    apply(json, None, None)
  }

  def apply(json: JsValue, packageName: Option[String], userAgent: Option[String]): ServiceDescription = {
    val internal = InternalServiceDescription(json)
    ServiceDescriptionBuilder(internal, packageName, userAgent)
  }

  def apply(internal: InternalServiceDescription, packageName: Option[String] = None, userAgent: Option[String] = None): ServiceDescription = {
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
      internal.description,
      userAgent
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

    val pathParameters = internal.namedPathParameters.map { name =>
      internal.parameters.find(_.name == Some(name)) match {
        case None => {
          ParameterBuilder.fromPath(model, name)
        }
        case Some(declared) => {
          // Path parameter was declared in the parameters
          // section. Use the explicit information provided in the
          // specification
          ParameterBuilder(enums, models, declared, ParameterLocation.Path)
        }
      }
    }

    val internalParams = internal.parameters.filter(p => pathParameters.find(_.name == p.name.get).isEmpty).map { p =>
      ParameterBuilder(enums, models, p, location)
    }

    Operation(model = model,
              method = method,
              path = internal.path,
              description = internal.description,
              body = internal.body.map { ib => Body(enums, models, ib) },
              parameters = pathParameters ++ internalParams,
              responses = internal.responses.map { ResponseBuilder(enums, models, _) })
  }

}

object Body {

  def apply(enums: Seq[Enum], models: Seq[Model], ib: InternalBody): Body = {
    ib.datatype match {
      case None => sys.error("Body missing type: " + ib)
      case Some(datatype) => com.gilt.apidocgenerator.models.Body(
        `type` = TypeResolver(
          enumNames = enums.map(_.name),
          modelNames = models.map(_.name)
        ).toTypeInstance(datatype).getOrElse {
          sys.error(s"Body[$datatype] is not valid")
        },
        description = ib.description
      )
    }
  }

}


object EnumBuilder {

  def apply(ie: InternalEnum): Enum = {
    Enum(
      name = ie.name,
      description = ie.description,
      values = ie.values.map { iv => EnumValue(name = iv.name.get, description = iv.description) }.to[Seq]
    )
  }

}

object HeaderBuilder {

  def apply(enums: Seq[Enum], ih: InternalHeader): Header = {
    Header(
      name = ih.name.get,
      `type` = TypeResolver(enumNames = enums.map(_.name)).toTypeInstance(ih.datatype.get).getOrElse {
        sys.error(s"Invalid header type[${ih.datatype.get.name}]")
      },
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
    Response(
      code = internal.code.toInt,
      `type` = TypeResolver(
        enumNames = enums.map(_.name),
        modelNames = models.map(_.name)
      ).toTypeInstance(internal.datatype.get).getOrElse {
        sys.error("No datatype for response: " + internal)
      }
    )
  }

}

object ParameterBuilder {

  def fromPath(model: Model, name: String): Parameter = {
    val typeInstance = model.fields.find(_.name == name).map(_.`type`).getOrElse {
      TypeInstance(Container.Singleton, Type(TypeKind.Primitive, Primitives.String.toString))
    }

    Parameter(name = name,
              `type` = typeInstance,
              location = ParameterLocation.Path,
              required = true)
  }

  def apply(enums: Seq[Enum], models: Seq[Model], internal: InternalParameter, location: ParameterLocation): Parameter = {
    val resolver = TypeResolver(
      enumNames = enums.map(_.name),
      modelNames = models.map(_.name)
    )
    val typeInstance = resolver.toTypeInstance(internal.datatype.get).getOrElse {
      sys.error("Could not resolve type for parameter: " + internal)
    }

    internal.default.map { ServiceDescriptionBuilderHelper.assertValidDefault(enums, typeInstance.`type`, _) }

    Parameter(name = internal.name.get,
              `type` = typeInstance,
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
    val typeInstance = TypeResolver(
      enumNames = enums.map(_.name)
    ).toTypeInstance(internal.datatype.get).getOrElse {
      TypeInstance(internal.datatype.get.container, Type(TypeKind.Model, internal.datatype.get.name))
    }

    internal.default.map { ServiceDescriptionBuilderHelper.assertValidDefault(enums, typeInstance.`type`, _) }

    Field(name = internal.name.get,
          `type` = typeInstance,
          description = internal.description,
          required = internal.required,
          default = internal.default,
          minimum = internal.minimum,
          maximum = internal.maximum,
          example = internal.example)
  }

}


object ServiceDescriptionBuilderHelper {

  def assertValidDefault(enums: Seq[Enum], t: Type, value: String) {
    TypeValidator(
      enums = enums.map(e => TypeValidatorEnums(e.name, e.values.map(_.name)))
    ).assertValidDefault(t, value)
  }

}
