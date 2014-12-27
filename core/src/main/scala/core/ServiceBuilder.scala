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
    val internal = InternalServiceForm(json)
    ServiceBuilder(internal, packageName, userAgent)
  }

  def apply(internal: InternalServiceForm, packageName: Option[String] = None, userAgent: Option[String] = None): Service = {
    val enums = internal.enums.map { EnumBuilder(_) }
    val models = internal.models.map { ModelBuilder(enums, _) }
    val headers = internal.headers.map { HeaderBuilder(enums, _) }
    val resources = internal.resources.map { ResourceBuilder(enums, models, _) }

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

  def apply(enums: Seq[Enum], models: Seq[Model], internal: InternalResourceForm): Resource = {
    val model = models.find(m => Some(m.name) == internal.modelName).getOrElse {
      sys.error(s"Could not find model for resource[${internal.modelName.getOrElse("")}]")
    }
    Resource(
      model = model,
      description = internal.description,
      operations = internal.operations.map(op => OperationBuilder(enums, models, model, op))
    )
  }

}

object OperationBuilder {

  def apply(enums: Seq[Enum], models: Seq[Model], model: Model, internal: InternalOperationForm): Operation = {
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
      path = internal.path,
      description = internal.description,
      body = internal.body.map { ib => BodyBuilder(enums, models, ib) },
      parameters = pathParameters ++ internalParams,
      responses = internal.responses.map { ResponseBuilder(enums, models, _) }
    )
  }

}

object BodyBuilder {

  def apply(enums: Seq[Enum], models: Seq[Model], ib: InternalBodyForm): Body = {
    ib.datatype match {
      case None => sys.error("Body missing type: " + ib)
      case Some(datatype) => Body(
        `type` = TypeResolver(
          enumNames = enums.map(_.name),
          modelNames = models.map(_.name)
        ).parseWithError(datatype).label,
        description = ib.description
      )
    }
  }

}


object EnumBuilder {

  def apply(ie: InternalEnumForm): Enum = {
    Enum(
      name = ie.name,
      description = ie.description,
      values = ie.values.map { iv => EnumValue(name = iv.name.get, description = iv.description) }
    )
  }

}

object HeaderBuilder {

  def apply(enums: Seq[Enum], ih: InternalHeaderForm): Header = {
    Header(
      name = ih.name.get,
      `type` = TypeResolver(enumNames = enums.map(_.name)).parseWithError(ih.datatype.get).label,
      required = ih.required,
      description = ih.description,
      default = ih.default
    )
  }

}

object ModelBuilder {

  def apply(enums: Seq[Enum], im: InternalModelForm): Model = {
    Model(
      name = im.name,
      plural = im.plural,
      description = im.description,
      fields = im.fields.map { FieldBuilder(enums, _) }
    )
  }

}

object ResponseBuilder {

  def apply(enums: Seq[Enum], models: Seq[Model], internal: InternalResponseForm): Response = {
    Response(
      code = internal.code.toInt,
      `type` = TypeResolver(
        enumNames = enums.map(_.name),
        modelNames = models.map(_.name)
      ).parseWithError(internal.datatype.get).label
    )
  }

}

object ParameterBuilder {

  def fromPath(model: Model, name: String): Parameter = {
    val datatypeLabel = model.fields.find(_.name == name).map(_.`type`).getOrElse {
      Datatype.Singleton(Seq(Type(TypeKind.Primitive, Primitives.String.toString))).label
    }

    Parameter(
      name = name,
      `type` = datatypeLabel,
      location = ParameterLocation.Path,
      required = true
    )
  }

  def apply(enums: Seq[Enum], models: Seq[Model], internal: InternalParameterForm, location: ParameterLocation): Parameter = {
    val resolver = TypeResolver(
      enumNames = enums.map(_.name),
      modelNames = models.map(_.name)
    )
    val typeInstance = resolver.parseWithError(internal.datatype.get)

    internal.default.map { ServiceBuilderHelper.assertValidDefault(enums, typeInstance, _) }

    Parameter(
      name = internal.name.get,
      `type` = typeInstance.label,
      location = location,
      description = internal.description,
      required = internal.required,
      default = internal.default,
      minimum = internal.minimum,
      maximum = internal.maximum,
      example = internal.example
    )
  }

}

object FieldBuilder {

  def apply(
    enums: Seq[Enum],
    internal: InternalFieldForm
  ): Field = {
    val datatype = TypeResolver(
      enumNames = enums.map(_.name),
      modelNames = internal.datatype.get.names // assume a model if not an enum
    ).parseWithError(internal.datatype.get)

    internal.default.map { ServiceBuilderHelper.assertValidDefault(enums, datatype, _) }

    Field(
      name = internal.name.get,
      `type` = datatype.label,
      description = internal.description,
      required = internal.required,
      default = internal.default,
      minimum = internal.minimum,
      maximum = internal.maximum,
      example = internal.example
    )
  }

}


object ServiceBuilderHelper {

  def assertValidDefault(enums: Seq[Enum], pd: Datatype, value: String) {
    TypeValidator(
      enums = enums.map { enum => TypeValidatorEnums(enum.name, enum.values.map(_.name)) }.toSeq
    ).assertValidDefault(pd, value)
  }

}
