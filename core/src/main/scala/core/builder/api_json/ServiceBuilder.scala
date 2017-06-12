package builder.api_json

import core.{Importer, ServiceFetcher, VersionMigration, TypesProvider, TypesProviderEnum, TypesProviderModel, TypesProviderUnion}
import lib.{Methods, Primitives, ServiceConfiguration, Text, Kind, UrlKey}
import com.bryzek.apidoc.spec.v0.models._
import play.api.libs.json._
import scala.util.{Failure, Success, Try}

case class ServiceBuilder(
  migration: VersionMigration
) {

  def apply(
    config: ServiceConfiguration,
    apiJson: String,
    fetcher: ServiceFetcher
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
    val resources = internal.resources.map { ResourceBuilder(resolver, _) }.sortWith(_.`type`.toLowerCase < _.`type`.toLowerCase)
    val attributes = internal.attributes.map { AttributeBuilder(_) }

    val info = internal.info match {
      case None => Info(
        contact = None,
        license = None
      )
      case Some(i) => InfoBuilder(i)
    }

    Service(
      apidoc = internal.apidoc.flatMap(_.version) match {
        case Some(v) => Apidoc(version = v)
        case None => Apidoc(version = com.bryzek.apidoc.spec.v0.Constants.Version)
      },
      info = info,
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
      resources = resources,
      attributes = attributes
    )
  }

  object ResourceBuilder {

    private[api_json] case class Resolution(
      enum: Option[TypesProviderEnum] = None,
      model: Option[TypesProviderModel] = None,
      union: Option[TypesProviderUnion] = None
    ) {
      private[this] val all = Seq(enum, model, union).flatten
      assert(all.size <= 1, s"Cannot have more than 1 resolved item: $all")

      def isEmpty: Boolean = all.isEmpty
    }

    // @scala.annotation.tailrec
    def resolve(
      resolver: TypesProvider,
      name: String
    ): Resolution = {
      resolver.enums.find(o => o.name == name || o.fullName == name) match {
        case Some(enum) => {
          Resolution(enum = Some(enum))
        }
        case None => {
          resolver.models.find(o => o.name == name || o.fullName == name) match {
            case Some(model) => {
              Resolution(model = Some(model))
            }
            case None => {
              resolver.unions.find(o => o.name == name || o.fullName == name) match {
                case Some(union) => {
                  Resolution(union = Some(union))
                }
                case None => {
                  Resolution()
                }
              }
            }
          }
        }
      }
    }

    def apply(
      resolver: TypeResolver,
      internal: InternalResourceForm
    ): Resource = {
      val resolution = resolve(resolver.provider, internal.datatype.name)

      resolution.enum match {
        case Some(enum) => {
          val resourcePath = internal.path.getOrElse("/" + enum.plural)
          Resource(
            `type` = internal.datatype.label,
            plural = enum.plural,
            path = Some(resourcePath),
            description = internal.description,
            deprecation = internal.deprecation.map(DeprecationBuilder(_)),
            operations = internal.operations.map(op => OperationBuilder(op, resourcePath, resolver)),
            attributes = internal.attributes.map { AttributeBuilder(_) }
          )
        }

        case None => {
          resolution.model match {
            case Some(model) => {
              val resourcePath = internal.path.getOrElse("/" + model.plural)
              Resource(
                `type` = internal.datatype.label,
                plural = model.plural,
                path = Some(resourcePath),
                description = internal.description,
                deprecation = internal.deprecation.map(DeprecationBuilder(_)),
                operations = internal.operations.map(op => OperationBuilder(op, resourcePath, resolver, model = Some(model))),
                attributes = internal.attributes.map { AttributeBuilder(_) }
              )
            }

            case None => {
              resolution.union match {
                case Some(union) => {
                  val resourcePath = internal.path.getOrElse("/" + union.plural)
                  Resource(
                    `type` = internal.datatype.label,
                    plural = union.plural,
                    path = Some(resourcePath),
                    description = internal.description,
                    deprecation = internal.deprecation.map(DeprecationBuilder(_)),
                    operations = internal.operations.map(op => OperationBuilder(op, resourcePath, resolver, union = Some(union))),
                    attributes = internal.attributes.map { AttributeBuilder(_) }
                  )
                }
                case None => {
                  val resourcePath = internal.path.getOrElse("")
                  Resource(
                    `type` = internal.datatype.label,
                    plural = Text.pluralize(internal.datatype.name),
                    path = Some(resourcePath),                    
                    description = internal.description,
                    deprecation = internal.deprecation.map(DeprecationBuilder(_)),
                    operations = internal.operations.map(op => OperationBuilder(op, resourcePath, resolver)),
                    attributes = internal.attributes.map { AttributeBuilder(_) }
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
      resourcePath: String,
      resolver: TypeResolver,
      model: Option[TypesProviderModel] = None,
      union: Option[TypesProviderUnion] = None
    ): Operation = {
      val method = internal.method.getOrElse("")
      val defaultLocation = if (!internal.body.isEmpty || !Methods.isJsonDocumentMethod(method)) { ParameterLocation.Query } else { ParameterLocation.Form }

      val pathParameters = internal.namedPathParameters.map { name =>
        internal.parameters.find(_.name == Some(name)) match {
          case None => {
            val datatypeLabel: String = model.flatMap(_.fields.find(_.name == name)) match {
              case Some(field) => field.`type`
              case None => {
                union.flatMap(commonField(resolver.provider, _, name)).getOrElse {
                  Primitives.String.toString
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

      val fullPath = Seq(
        resourcePath,
        internal.path.getOrElse("")
      ).filter(!_.isEmpty).mkString("") match {
        case "" => "/"
        case p => p
      }

      Operation(
        method = Method(method),
        path = fullPath,
        description = internal.description,
        deprecation = internal.deprecation.map(DeprecationBuilder(_)),
        body = internal.body.map { BodyBuilder(_) },
        parameters = pathParameters ++ internalParams,
        responses = internal.responses.map { ResponseBuilder(resolver, _) },
        attributes = internal.attributes.map { AttributeBuilder(_) }
      )
    }

    /**
      * If all types agree on the datatype for the field with the specified Name,
      * returns the field. Otherwise, returns None
      */
    private def commonField(resolver: TypesProvider, union: TypesProviderUnion, fieldName: String): Option[String] = {
      val fieldTypes: Seq[String] = union.types.map { u =>
        Primitives(u.`type`) match {
          case Some(p) => p.toString
          case None => {
            resolver.models.find { m => m.name == u.`type` || m.fullName == u.`type` } match {
              case None => {
                Primitives.String.toString
              }
              case Some(m) => {
                m.fields.find(_.name == fieldName) match {
                  case None => {
                    Primitives.String.toString
                  }
                  case Some(f) => {
                    f.`type`
                  }
                }
              }
            }
          }
        }
      }

      fieldTypes.distinct.toList match {
        case single :: Nil => Some(single)
        case _ => None
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
          deprecation = ib.deprecation.map(DeprecationBuilder(_)),
          attributes = ib.attributes.map { AttributeBuilder(_) }
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
        },
        attributes = ie.attributes.map { AttributeBuilder(_) }
      )
    }

  }

  object UnionBuilder {

    def apply(internal: InternalUnionForm): Union = {
      Union(
        name = internal.name,
        plural = internal.plural,
        discriminator = internal.discriminator,
        description = internal.description,
        deprecation = internal.deprecation.map(DeprecationBuilder(_)),
        types = internal.types.map { it =>
          UnionType(
            `type` = it.datatype.get.label,
            description = it.description,
            deprecation = it.deprecation.map(DeprecationBuilder(_)),
            default = it.default
          )
        },
        attributes = internal.attributes.map { AttributeBuilder(_) }
      )
    }

  }

  object HeaderBuilder {

    def apply(resolver: TypeResolver, ih: InternalHeaderForm): Header = {
      Header(
        name = ih.name.get,
        `type` = resolver.parseWithError(ih.datatype.get).toString,
        required = ih.required,
        description = ih.description,
        deprecation = ih.deprecation.map(DeprecationBuilder(_)),
        default = ih.default,
        attributes = ih.attributes.map { AttributeBuilder(_) }
      )
    }

  }

  object InfoBuilder {

    def apply(internal: InternalInfoForm): Info = {
      Info(
        contact = internal.contact.flatMap { c =>
          if (c.name.isEmpty && c.email.isEmpty && c.url.isEmpty) {
            None
          } else {
            Some(
              Contact(
                name = c.name,
                url = c.url,
                email = c.email
              )
            )
          }
        },
        license = internal.license.map { l =>
          License(
            name = l.name.getOrElse {
              sys.error("License is missing name")
            },
            url = l.url
          )
        }
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
        fields = im.fields.map { FieldBuilder(_) },
        attributes = im.attributes.map { AttributeBuilder(_) }
      )
    }

  }

  object ResponseBuilder {

    def apply(resolver: TypeResolver, internal: InternalResponseForm): Response = {
      Response(
        code = Try(internal.code.toInt) match {
          case Success(code) => ResponseCodeInt(code)
          case Failure(ex) => ResponseCodeOption(internal.code)
        },
        `type` = internal.datatype.get.label,
        headers = internal.headers.map { HeaderBuilder(resolver, _) }.toList match {
          case Nil => None
          case headers => Some(headers)
        },
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
        required = migration.makeFieldsWithDefaultsRequired match {
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
        required = migration.makeFieldsWithDefaultsRequired match {
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
        example = internal.example,
        attributes = internal.attributes.map { AttributeBuilder(_) }
      )
    }

  }

  object AttributeBuilder {
    def apply(
      internal: InternalAttributeForm
    ): Attribute = {
      Attribute(
        name = internal.name.get,
        value = internal.value.get,
        description = internal.description,
        deprecation = internal.deprecation.map(DeprecationBuilder(_))
      )
    }

  }

}
