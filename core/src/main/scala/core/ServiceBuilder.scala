package core

import lib.{Datatype, Primitives, Type, TypeKind}
import com.gilt.apidocspec.models._
import play.api.libs.json._
import org.joda.time.format.ISODateTimeFormat

object ServiceBuilder {

  def apply(apiJson: String): Service = {
    apply(apiJson, None, None)
  }

  def apply(apiJson: String, packageName: Option[String], userAgent: Option[String]): Service = {
    val jsValue = Json.parse(apiJson)
    ServiceBuilder(jsValue, packageName, userAgent)
  }

  def apply(json: JsValue): Service = {
    apply(json, None, None)
  }

  def apply(json: JsValue, packageName: Option[String], userAgent: Option[String]): Service = {
    val internal = InternalService(json)
    ServiceBuilder(internal, packageName, userAgent)
  }

  def apply(internal: InternalService, packageName: Option[String] = None, userAgent: Option[String] = None): Service = {
    val enums = internal.enums.map { enum => (enum.name -> EnumBuilder(enum)) }.toMap
    val models = internal.models.map { model => (model.name -> ModelBuilder(enums, model)) }.toMap
    val headers = internal.headers.map(HeaderBuilder(enums, _))
    val resources = internal.resources.map { resource => (resource.modelName.get -> ResourceBuilder(enums, models, resource)) }.toMap

    // TODO: packageName, userAgent

    Service(
      name = internal.name.getOrElse(sys.error("Missing name")),
      description = internal.description,
      baseUrl = internal.baseUrl,
      enums = enums,
      models = models,
      headers = headers,
      resources = resources
    )
  }
}

object ResourceBuilder {

  def apply(enums: Map[String, Enum], models: Map[String, Model], internal: InternalResource): Resource = {
    val model = models.get(internal.modelName.get).getOrElse {
      sys.error(s"Could not find model for resource[${internal.modelName.getOrElse("")}]")
    }
    Resource(
      path = Some(internal.path),
      operations = internal.operations.map(op => OperationBuilder(enums, models, model, op))
    )
  }

}

object OperationBuilder {

  def apply(enums: Map[String, Enum], models: Map[String, Model], model: Model, internal: InternalOperation): Operation = {
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

    Operation(
      method = Method(method),
      path = Some(internal.path),
      description = internal.description,
      body = internal.body.map { ib => Body(enums, models, ib) },
      parameters = pathParameters ++ internalParams,
      responses = internal.responses.map { response => (response.code -> ResponseBuilder(enums, models, response)) }.toMap
    )
  }

}

object Body {

  def apply(enums: Map[String, Enum], models: Map[String, Model], ib: InternalBody): Body = {
    ib.datatype match {
      case None => sys.error("Body missing type: " + ib)
      case Some(datatype) => com.gilt.apidocspec.models.Body(
        `type` = TypeResolver(
          enumNames = enums.keys,
          modelNames = models.keys
        ).parseWithError(datatype).label,
        description = ib.description
      )
    }
  }

}


object EnumBuilder {

  def apply(ie: InternalEnum): Enum = {
    Enum(
      description = ie.description,
      values = ie.values.map { iv => EnumValue(name = iv.name.get, description = iv.description) }
    )
  }

}

object HeaderBuilder {

  def apply(enums: Map[String, Enum], ih: InternalHeader): Header = {
    Header(
      name = ih.name.get,
      `type` = TypeResolver(enumNames = enums.keys).parseWithError(ih.datatype.get).label,
      required = ih.required,
      description = ih.description,
      default = ih.default
    )
  }

}

object ModelBuilder {

  def apply(enums: Map[String, Enum], im: InternalModel): Model = {
    Model(
      plural = Some(im.plural),
      description = im.description,
      fields = im.fields.map { FieldBuilder(enums, im, _) }
    )
  }

}

object ResponseBuilder {

  def apply(enums: Map[String, Enum], models: Map[String, Model], internal: InternalResponse): Response = {
    Response(
      `type` = TypeResolver(
        enumNames = enums.keys,
        modelNames = models.keys
      ).parseWithError(internal.datatype.get).label
    )
  }

}

object ParameterBuilder {

  def fromPath(model: Model, name: String): Parameter = {
    val datatypeLabel = model.fields.find(_.name == name).map(_.`type`).getOrElse {
      Datatype.Singleton(Type(TypeKind.Primitive, Primitives.String.toString)).label
    }

    Parameter(
      name = name,
      `type` = datatypeLabel,
      location = Some(ParameterLocation.Path),
      required = Some(true)
    )
  }

  def apply(enums: Map[String, Enum], models: Map[String, Model], internal: InternalParameter, location: ParameterLocation): Parameter = {
    val resolver = TypeResolver(
      enumNames = enums.keys,
      modelNames = models.keys
    )
    val typeInstance = resolver.parseWithError(internal.datatype.get)

    internal.default.map { ServiceBuilderHelper.assertValidDefault(enums, typeInstance, _) }

    Parameter(
      name = internal.name.get,
      `type` = typeInstance.label,
      location = Some(location),
      description = internal.description,
      required = Some(internal.required),
      default = internal.default,
      minimum = internal.minimum,
      maximum = internal.maximum,
      example = internal.example
    )
  }

}

object FieldBuilder {

  def apply(enums: Map[String, Enum], im: InternalModel, internal: InternalField): Field = {
    val datatype = TypeResolver(
      enumNames = enums.keys,
      modelNames = internal.datatype.get.names // assume a model if not an enum
    ).parseWithError(internal.datatype.get)

    internal.default.map { ServiceBuilderHelper.assertValidDefault(enums, datatype, _) }

    Field(
      name = internal.name.get,
      `type` = datatype.label,
      description = internal.description,
      required = Some(internal.required),
      default = internal.default,
      minimum = internal.minimum,
      maximum = internal.maximum,
      example = internal.example
    )
  }

}


object ServiceBuilderHelper {

  def assertValidDefault(enums: Map[String, Enum], pd: Datatype, value: String) {
    TypeValidator(
      enums = enums.map { case(enumName, enum) => TypeValidatorEnums(enumName, enum.values.map(_.name)) }.toSeq
    ).assertValidDefault(pd, value)
  }

}
