package core

import lib.{Datatype, Methods, Primitives, Type, Kind, UrlKey}
import com.gilt.apidoc.spec.v0.models._
import play.api.libs.json._

object ServiceBuilder {

  def apply(
    config: ServiceConfiguration,
    apiJson: String,
    fetcher: ServiceFetcher = ClientFetcher()
  ): Service = {
    val jsValue = Json.parse(apiJson)
    apply(config, InternalServiceForm(jsValue, fetcher))
  }

  def apply(
    config: ServiceConfiguration,
    internal: InternalServiceForm
  ): Service = {
    val resolver = TypeResolver(
      RecursiveTypesProvider(internal)
    )

    val name = internal.name.getOrElse(sys.error("Missing name"))
    val key = internal.key.getOrElse { UrlKey.generate(name) }
    val namespace = internal.namespace.getOrElse { config.applicationNamespace(key) }
    val imports = internal.imports.map { ImportBuilder(internal.fetcher, _) }.sortWith(_.uri.toLowerCase < _.uri.toLowerCase)
    val headers = internal.headers.map { HeaderBuilder(resolver, _) }
    val enums = internal.enums.map { EnumBuilder(_) }.sortWith(_.name.toLowerCase < _.name.toLowerCase)
    val unions = internal.unions.map { UnionBuilder(_) }.sortWith(_.name.toLowerCase < _.name.toLowerCase)
    val models = internal.models.map { ModelBuilder(resolver, _) }.sortWith(_.name.toLowerCase < _.name.toLowerCase)
    val resources = internal.resources.map { ResourceBuilder(resolver, models, enums, unions, _) }.sortWith(_.`type`.toLowerCase < _.`type`.toLowerCase)

    Service(
      name = name,
      namespace = namespace,
      organization = Organization(key = config.orgKey),
      application = Application(key = key),
      version = config.version,
      imports = imports,
      description = internal.description,
      baseUrl = internal.baseUrl,
      enums = enums,
      unions = unions,
      models = models,
      headers = headers,
      resources = resources
    )
  }
}

object ResourceBuilder {

  def apply(
    resolver: TypeResolver,
    models: Seq[Model],
    enums: Seq[Enum],
    unions: Seq[Union],
    internal: InternalResourceForm
  ): Resource = {
    enums.find(_.name == internal.datatype.name) match {
      case Some(enum) => {
        Resource(
          `type` = internal.datatype.name,
          plural = enum.plural,
          description = internal.description,
          operations = internal.operations.map(op => OperationBuilder(resolver, op))
        )
      }

      case None => {
        models.find(_.name == internal.datatype.name) match {
          case Some(model) => {
            Resource(
              `type` = internal.datatype.name,
              plural = model.plural,
              description = internal.description,
              operations = internal.operations.map(op => OperationBuilder(resolver, op, model = Some(model)))
            )
          }

          case None => {
            unions.find(_.name == internal.datatype.name) match {
              case None => {
                sys.error(s"Resource type[${internal.datatype.name}] must resolve to a model, enum or union type")
              }
              case Some(union) => {
                Resource(
                  `type` = internal.datatype.name,
                  plural = union.plural,
                  description = internal.description,
                  operations = internal.operations.map(op => OperationBuilder(resolver, op, union = Some(union), models = models))
                )
              }
            }
          }
        }
      }
    }
  }

}

object OperationBuilder {

  def apply(
    resolver: TypeResolver,
    internal: InternalOperationForm,
    model: Option[Model] = None,
    union: Option[Union] = None,
    models: Seq[Model] = Nil
  ): Operation = {
    val method = internal.method.getOrElse { sys.error("Missing method") }
    val location = if (!internal.body.isEmpty || !Methods.isJsonDocumentMethod(method)) { ParameterLocation.Query } else { ParameterLocation.Form }

    val pathParameters = internal.namedPathParameters.map { name =>
      internal.parameters.find(_.name == Some(name)) match {
        case None => {
          val datatypeLabel: String = model.flatMap(_.fields.find(_.name == name)) match {
            case Some(field) => field.`type`
            case None => {
              union.flatMap(commonField(_, models, name)) match {
                case Some(field) => field.`type`
                case None => Primitives.String.toString
              }
            }
          }

          ParameterBuilder.fromPath(name, datatypeLabel)
        }
        case Some(declared) => {
          // Path parameter was declared in the parameters
          // section. Use the explicit information provided in the
          // specification
          ParameterBuilder(resolver, declared, ParameterLocation.Path)
        }
      }
    }

    val internalParams = internal.parameters.filter(p => pathParameters.find(_.name == p.name.get).isEmpty).map { p =>
      ParameterBuilder(resolver, p, location)
    }

    Operation(
      method = Method(method),
      path = internal.path,
      description = internal.description,
      body = internal.body.map { ib => BodyBuilder(resolver, ib) },
      parameters = pathParameters ++ internalParams,
      responses = internal.responses.map { ResponseBuilder(resolver, _) }
    )
  }

  /**
    * If all types agree on the datatype for the field with the specified Name,
    * returns the field. Otherwise, returns None
    */
  private def commonField(union: Union, models: Seq[Model], fieldName: String): Option[Field] = {
    val unionModels = union.types.flatMap(u => models.find(_.name == u.`type`))
    unionModels.flatMap(_.fields.find(_.name == fieldName)) match {
      case Nil => None
      case fields => {
        fields.map(_.`type`).distinct.toList match {
          case single :: Nil => fields.headOption
          case _ => None
        }
      }
    }
  }

}

object BodyBuilder {

  def apply(resolver: TypeResolver, ib: InternalBodyForm): Body = {
    ib.datatype match {
      case None => sys.error("Body missing type: " + ib)
      case Some(datatype) => Body(
        `type` = resolver.parseWithError(datatype).label,
        description = ib.description
      )
    }
  }

}

object DeprecationBuilder {

  def apply(internal: InternalDeprecationForm): Deprecation = {
    Deprecation(description = internal.description)
  }

}

object EnumBuilder {

  def apply(ie: InternalEnumForm): Enum = {
    Enum(
      name = ie.name,
      plural = ie.plural,
      description = ie.description,
      deprecation = ie.deprecation.map(DeprecationBuilder(_)),
      values = ie.values.map { iv =>
        EnumValue(
          name = iv.name.get,
          description = iv.description,
          deprecation = iv.deprecation.map(DeprecationBuilder(_))
        )
      }
    )
  }

}

object UnionBuilder {

  def apply(internal: InternalUnionForm): Union = {
    Union(
      name = internal.name,
      plural = internal.plural,
      description = internal.description,
      deprecation = internal.deprecation.map(DeprecationBuilder(_)),
      types = internal.types.map { it =>
        UnionType(
          `type` = it.datatype.get.label,
          description = it.description,
          deprecation = it.deprecation.map(DeprecationBuilder(_))
        )
      }
    )
  }

}

object HeaderBuilder {

  def apply(resolver: TypeResolver, ih: InternalHeaderForm): Header = {
    Header(
      name = ih.name.get,
      `type` = resolver.parseWithError(ih.datatype.get).label,
      required = ih.required,
      description = ih.description,
      default = ih.default
    )
  }

}

object ImportBuilder {

  def apply(fetcher: ServiceFetcher, internal: InternalImportForm): Import = {
    val importer = Importer(fetcher, internal.uri.get)
    val service = importer.service

    Import(
      uri = internal.uri.get,
      organization = service.organization,
      application = service.application,
      namespace = service.namespace,
      version = service.version,
      enums = service.enums.map(_.name),
      unions = service.unions.map(_.name),
      models = service.models.map(_.name)
    )
  }

}

object ModelBuilder {

  def apply(resolver: TypeResolver, im: InternalModelForm): Model = {
    Model(
      name = im.name,
      plural = im.plural,
      description = im.description,
      fields = im.fields.map { FieldBuilder(resolver, _) }
    )
  }

}

object ResponseBuilder {

  def apply(resolver: TypeResolver, internal: InternalResponseForm): Response = {
    Response(
      code = internal.code.toInt,
      `type` = resolver.parseWithError(internal.datatype.get).label
    )
  }

}

object ParameterBuilder {

  def fromPath(name: String, datatypeLabel: String): Parameter = {
    Parameter(
      name = name,
      `type` = datatypeLabel,
      location = ParameterLocation.Path,
      required = true
    )
  }

  def apply(resolver: TypeResolver, internal: InternalParameterForm, location: ParameterLocation): Parameter = {
    val typeInstance = resolver.parseWithError(internal.datatype.get)

    internal.default.map { resolver.assertValidDefault(typeInstance, _) }

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
    resolver: TypeResolver,
    internal: InternalFieldForm
  ): Field = {
    val datatype = resolver.parseWithError(internal.datatype.get)

    internal.default.map { resolver.assertValidDefault(datatype, _) }

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
