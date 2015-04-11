package builder.api_json

import core.{ClientFetcher, Importer, ServiceFetcher}
import lib.{Datatype, Methods, Primitives, ServiceConfiguration, Text, Type, Kind, UrlKey}
import com.gilt.apidoc.spec.v0.models._
import play.api.libs.json._
import scala.util.{Failure, Success, Try}

case class ServiceBuilder(
  internalMigration: Boolean = false
) {

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

    val name = internal.name.getOrElse(sys.error("Missing name"))
    val key = internal.key.getOrElse { UrlKey.generate(name) }
    val namespace = internal.namespace.getOrElse { config.applicationNamespace(key) }

    val resolver = TypeResolver(
      defaultNamespace = Some(namespace),
      RecursiveTypesProvider(internal)
    )

    val imports = internal.imports.map { ImportBuilder(internal.fetcher, _) }.sortWith(_.uri.toLowerCase < _.uri.toLowerCase)
    val headers = internal.headers.map { HeaderBuilder(resolver, _) }
    val enums = internal.enums.map { EnumBuilder(_) }.sortWith(_.name.toLowerCase < _.name.toLowerCase)
    val unions = internal.unions.map { UnionBuilder(_) }.sortWith(_.name.toLowerCase < _.name.toLowerCase)
    val models = internal.models.map { ModelBuilder(_) }.sortWith(_.name.toLowerCase < _.name.toLowerCase)
    val resources = internal.resources.map { ResourceBuilder(models, enums, unions, _) }.sortWith(_.`type`.toLowerCase < _.`type`.toLowerCase)

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

  object ResourceBuilder {

    def apply(
      models: Seq[Model],
      enums: Seq[Enum],
      unions: Seq[Union],
      internal: InternalResourceForm
    ): Resource = {
      enums.find(_.name == internal.datatype.name) match {
        case Some(enum) => {
          Resource(
            `type` = internal.datatype.label,
            plural = enum.plural,
            description = internal.description,
            deprecation = internal.deprecation.map(DeprecationBuilder(_)),
            operations = internal.operations.map(op => OperationBuilder(op, models = models))
          )
        }

        case None => {
          models.find(_.name == internal.datatype.name) match {
            case Some(model) => {
              Resource(
                `type` = internal.datatype.label,
                plural = model.plural,
                description = internal.description,
                deprecation = internal.deprecation.map(DeprecationBuilder(_)),
                operations = internal.operations.map(op => OperationBuilder(op, model = Some(model)))
              )
            }

            case None => {
              unions.find(_.name == internal.datatype.name) match {
                case None => {
                  Resource(
                    `type` = internal.datatype.label,
                    plural = Text.pluralize(internal.datatype.name),
                    description = internal.description,
                    deprecation = internal.deprecation.map(DeprecationBuilder(_)),
                    operations = internal.operations.map(op => OperationBuilder(op, models = models))
                  )
                }
                case Some(union) => {
                  Resource(
                    `type` = internal.datatype.label,
                    plural = union.plural,
                    description = internal.description,
                    deprecation = internal.deprecation.map(DeprecationBuilder(_)),
                    operations = internal.operations.map(op => OperationBuilder(op, union = Some(union), models = models))
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
      internal: InternalOperationForm,
      model: Option[Model] = None,
      union: Option[Union] = None,
      models: Seq[Model] = Nil
    ): Operation = {
      val method = internal.method.getOrElse("")
      val defaultLocation = if (!internal.body.isEmpty || !Methods.isJsonDocumentMethod(method)) { ParameterLocation.Query } else { ParameterLocation.Form }

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
            ParameterBuilder(declared, ParameterLocation.Path)
          }
        }
      }

      val internalParams = internal.parameters.filter(p => pathParameters.find(_.name == p.name.get).isEmpty).map { p =>
        ParameterBuilder(p, defaultLocation)
      }

      Operation(
        method = Method(method),
        path = internal.path,
        description = internal.description,
        deprecation = internal.deprecation.map(DeprecationBuilder(_)),
        body = internal.body.map { BodyBuilder(_) },
        parameters = pathParameters ++ internalParams,
        responses = internal.responses.map { ResponseBuilder(_) }
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

    def apply(ib: InternalBodyForm): Body = {
      ib.datatype match {
        case None => sys.error("Body missing type: " + ib)
        case Some(datatype) => Body(
          `type` = datatype.label,
          description = ib.description,
          deprecation = ib.deprecation.map(DeprecationBuilder(_))
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
        deprecation = ih.deprecation.map(DeprecationBuilder(_)),
        default = ih.default
      )
    }

  }

  object ImportBuilder {

    def apply(fetcher: ServiceFetcher, internal: InternalImportForm): Import = {
      Importer(fetcher, internal.uri.get).fetched match {
        case Left(errors) => {
          sys.error("Errors in import: " + errors)
        }
        case Right(service) => {
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
    }

  }

  object ModelBuilder {

    def apply(im: InternalModelForm): Model = {
      Model(
        name = im.name,
        plural = im.plural,
        description = im.description,
        deprecation = im.deprecation.map(DeprecationBuilder(_)),
        fields = im.fields.map { FieldBuilder(_) }
      )
    }

  }

  object ResponseBuilder {

    def apply(internal: InternalResponseForm): Response = {
      Response(
        code = Try(internal.code.toInt) match {
          case Success(code) => ResponseCodeInt(code)
          case Failure(ex) => ResponseCodeOption(internal.code)
        },
        `type` = internal.datatype.get.label,
        description = internal.description,
        deprecation = internal.deprecation.map(DeprecationBuilder(_))
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

    def apply(internal: InternalParameterForm, defaultLocation: ParameterLocation): Parameter = {
      Parameter(
        name = internal.name.get,
        `type` = internal.datatype.get.label,
        location = internal.location.map(ParameterLocation(_)).getOrElse(defaultLocation),
        description = internal.description,
        deprecation = internal.deprecation.map(DeprecationBuilder(_)),
        required = internalMigration match {
          case true => {
            internal.default match {
              case None => internal.required
              case Some(_) => true
            }
          }
          case false => internal.required
        },
        default = internal.default,
        minimum = internal.minimum,
        maximum = internal.maximum,
        example = internal.example
      )
    }

  }

  object FieldBuilder {
    def apply(
      internal: InternalFieldForm
    ): Field = {
      Field(
        name = internal.name.get,
        `type` = internal.datatype.get.label,
        description = internal.description,
        deprecation = internal.deprecation.map(DeprecationBuilder(_)),
        required = internalMigration match {
          case true => {
            internal.default match {
              case None => internal.required
              case Some(_) => true
            }
          }
          case false => internal.required
        },
        default = internal.default,
        minimum = internal.minimum,
        maximum = internal.maximum,
        example = internal.example
      )
    }

  }

}
