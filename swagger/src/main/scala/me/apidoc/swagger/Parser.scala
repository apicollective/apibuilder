package me.apidoc.swagger

import translators.Resolver
import lib.{ServiceConfiguration, Text, UrlKey}
import java.io.File
import scala.collection.JavaConversions._
import play.api.libs.json.{Json, JsArray, JsObject, JsString, JsValue}

import io.swagger.parser.SwaggerParser
import com.wordnik.swagger.models.{ComposedModel, ModelImpl, RefModel, Swagger}
import com.wordnik.swagger.models.{parameters => swaggerparams}
import com.wordnik.swagger.models.properties.{AbstractNumericProperty, ArrayProperty, Property, RefProperty, StringProperty, UUIDProperty}
import com.gilt.apidoc.spec.v0.models._

import lib.Text
import com.gilt.apidoc.spec.v0.models._
import scala.annotation.tailrec

case class Parser(config: ServiceConfiguration) {

  def parseString(
    contents: String
  ): Service = {
    parse(Util.writeToTempFile(contents))
  }

  def parse(
    path: File
  ): Service = {
    val swagger = new SwaggerParser().read(path.toString)
    val info = swagger.getInfo()
    val applicationKey = UrlKey.generate(info.getTitle())
    val specModels = models(swagger)
    val resolver = Resolver(models = specModels)

    Service(
      name = info.getTitle(),
      description = Option(info.getDescription()),
      baseUrl = Converters.baseUrls(Util.toArray(swagger.getSchemes).map(_.toString), swagger.getHost, Option(swagger.getBasePath)).headOption,
      namespace = config.applicationNamespace(applicationKey),
      organization = Organization(key = config.orgKey),
      application = Application(key = applicationKey),
      version = config.version,
      enums = Nil,
      unions = Nil,
      models = specModels,
      imports = Nil,
      headers = Nil,
      resources = mergeResources(resources(swagger, resolver))
    )
  }

  private case class MyDefinition(name: String, definition: com.wordnik.swagger.models.Model)

  private def models(swagger: Swagger): Seq[Model] = {
    buildModels(
      resolver = Resolver(models = Nil),
      definitions = swagger.getDefinitions.map { case (name, definition) => MyDefinition(name, definition) }.toSeq
    )
  }

  private def nextDefinition(
    resolver: Resolver,
    definitions: Seq[MyDefinition]
  ): Option[MyDefinition] = {
    definitions.headOption // TODO
  }

  @tailrec
  private def buildModels(
    resolver: Resolver,
    definitions: Seq[MyDefinition],
    numIterations: Int = 0
  ): Seq[Model] = {
    nextDefinition(resolver, definitions) match {
      case None => {
        definitions.toList match {
          case Nil => resolver.models
          case remaining => sys.error("Failed to resolve definitions: " + definitions.map(_.name).mkString(", "))
        }
      }

      case Some(mydefinition) => {
        val name = mydefinition.name
        val newModel = mydefinition.definition match {
          case m: ComposedModel => {
            var composedModel: Option[Model] = None

            m.getInterfaces.foreach { i =>
              sys.error("TODO: Handle interfaces: " + i)
            }

            m.getAllOf.foreach { swaggerModel =>
              val thisModel = swaggerModel match {
                case rm: RefModel => resolver.resolveWithError(rm)
                case m: ModelImpl => toModel(resolver, name, m)
                case _ => sys.error(s"Unsupported composition model[$swaggerModel]")
              }

              composedModel = composedModel match {
                case None => Some(
                  thisModel.copy(
                    name = name,
                    plural = Text.pluralize(name)
                  )
                )
                case Some(cm) => Some(composeModels(cm, thisModel))
              }
            }

            composedModel.getOrElse {
              sys.error(s"Empty composed model: $name")
            }
          }

          case rm: RefModel => resolver.resolveWithError(rm)
          case m: ModelImpl => toModel(resolver, name, m)
          case _ => sys.error(s"Unsupported definition for name[$name]")
        }

        buildModels(
          resolver = Resolver(models = resolver.models ++ Seq(newModel)),
          definitions.filter(_.name != name),
          numIterations = numIterations + 1
        )
      }
    }
  }

  // @tailrec
  private def mergeResources(resources: Seq[Resource]): Seq[Resource] = {
    resources.groupBy(_.`type`).flatMap {
      case (resourceType, resources) => {
        resources.toList match {
          case Nil => Nil
          case resource :: Nil => Seq(resource)
          case r1 :: r2 :: Nil => Seq(mergeResourcesIntoOne(r1, r2))
          case r1 :: r2 :: rest => mergeResources(Seq(mergeResourcesIntoOne(r1, r2)) ++ rest)
        }
      }
    }.toSeq
  }

  private def mergeResourcesIntoOne(r1: Resource, r2: Resource): Resource = {
    r1.copy(
      description = Util.choose(r1.description, r2.description),
      deprecation = Util.choose(r1.deprecation, r2.deprecation),
      operations = r1.operations ++ r2.operations
    )
  }

  private def composeModels(m1: Model, m2: Model): Model = {
    m1.copy(
      description = Util.choose(m2.description, m1.description),
      deprecation = Util.choose(m2.deprecation, m1.deprecation),
      fields = m1.fields.map { f =>
        m2.fields.find(_.name == f.name) match {
          case None => f
          case Some(other) => composeFields(f, other)
        }
      } ++ m2.fields.filter( f => m1.fields.find(_.name == f.name).isEmpty )
    )
  }

  private def composeFields(f1: Field, f2: Field): Field = {
    f1.copy(
      `type` = f2.`type`,
      description = Util.choose(f2.description, f1.description),
      deprecation = Util.choose(f2.deprecation, f1.deprecation),
      default = Util.choose(f2.default, f1.default),
      required = f2.required,
      minimum = Util.choose(f2.minimum, f1.minimum),
      maximum = Util.choose(f2.maximum, f1.maximum),
      example = Util.choose(f2.example, f1.example)
    )
  }

  private def toModel(
    resolver: Resolver,
    name: String,
    m: ModelImpl
  ): Model = {
    // TODO println("  - type: " + Option(schema.getType()))
    // TODO println("  - discriminator: " + Option(schema.getDiscriminator()))

    val desc = Converters.combine(
      Seq(
        Option(m.getDescription()),
        externalDocsToString(Option(m.getExternalDocs))
      )
    )

    Option(m.getAdditionalProperties()).map { prop =>
      // TODO: println("    - additional property: " + prop)
    }

    Model(
      name = name,
      plural = Text.pluralize(name),
      description = desc,
      deprecation = None,
      fields = Util.toMap(m.getProperties).map {
        case (key, prop) => {
          val base = field(resolver, key, prop)

          prop match {
            case p: ArrayProperty => {
              // TODO: p.getUniqueItems()
              base.copy(`type` = "[" + base.`type` + "]")
            }
            case p: AbstractNumericProperty => {
              base.copy(
                minimum = Option(p.getMinimum()).map(_.toLong) match {
                  case None => Option(p.getExclusiveMinimum()).map(_.toLong)
                  case Some(v) => Some(v)
                },
                maximum = Option(p.getMaximum()).map(_.toLong) match {
                  case None => Option(p.getExclusiveMaximum()).map(_.toLong)
                  case Some(v) => Some(v)
                }
              )
            }
            case p: UUIDProperty => {
              base.copy(
                minimum = Option(p.getMinLength()).map(_.toLong),
                maximum = Option(p.getMaxLength()).map(_.toLong)
              )
            }
            case p: StringProperty => {
              // TODO getPattern
              val desc = Option(p.getPattern) match {
                case None => base.description
                case Some(pattern) => Converters.combine(Seq(base.description, Some(s"Pattern: $pattern")))
              }

              base.copy(
                description = desc,
                minimum = Option(p.getMinLength()).map(_.toLong),
                maximum = Option(p.getMaxLength()).map(_.toLong)
              )
            }
            case _ => base
          }
        }
      }.toSeq
    )
  }

  private def externalDocsToString(docs: Option[com.wordnik.swagger.models.ExternalDocs]): Option[String] = {
    docs.flatMap { doc =>
      Converters.combine(Seq(Option(doc.getDescription), Option(doc.getUrl)), ": ")
    }
  }

  private def field(
    resolver: Resolver,
    name: String,
    prop: Property
): Field = {
    // Ignoring:
    // println(s"    - readOnly: " + Option(prop.getReadOnly()))
    // println(s"    - xml: " + Option(prop.getXml()))
    Field(
      name = name,
      description = Option(prop.getDescription()),
      `type` = resolver.schemaType(prop),
      required = prop.getRequired(),
      example = Option(prop.getExample())
    )
  }

  private def resources(
    swagger: Swagger,
    resolver: Resolver
  ): Seq[Resource] = {
    swagger.getPaths.map {
      case (url, p) => {
        val model = findModelByUrl(resolver.models, url).getOrElse {
          sys.error(s"Could not find model at url[$url]")
        }

        val operations = Seq(
          Option(p.getGet).map { toOperation(resolver, Method.Get, url, _) },
          Option(p.getPost).map { toOperation(resolver, Method.Post, url, _) },
          Option(p.getPut).map { toOperation(resolver, Method.Put, url, _) },
          Option(p.getDelete).map { toOperation(resolver, Method.Delete, url, _) },
          Option(p.getOptions).map { toOperation(resolver, Method.Options, url, _) },
          Option(p.getPatch).map { toOperation(resolver, Method.Patch, url, _) }
        ).flatten

        // getVendorExtensions
        // getParameters

        Resource(
          `type` = model.name,
          plural = model.plural,
          description = None,
          deprecation = None,
          operations = operations
        )
      }
    }.toSeq
  }

  private def toOperation(
    resolver: Resolver,
    method: Method,
    url: String,
    op: com.wordnik.swagger.models.Operation
  ): Operation = {
    // println("  - tags: " + Util.toArray(op.getTags()).mkString(", "))

    val summary = Option(op.getSummary())
    val description = Option(op.getDescription())

    val parameters = Util.toArray(op.getParameters).flatMap { param =>
      param match {
        case p: com.wordnik.swagger.models.parameters.BodyParameter => None
        case _ => Some(toParameter(resolver, param))
      }
    }

    val bodies = Util.toArray(op.getParameters).flatMap { param =>
      param match {
        case p: com.wordnik.swagger.models.parameters.BodyParameter => Some(translators.Body(resolver, p))
        case _ => None
      }
    }

    val responses = op.getResponses.map {
      case (code, swaggerResponse) => {
        translators.Response(resolver, code, swaggerResponse)
      }
    }

    // println("  - schemes: " + Util.toArray(op.getSchemes()).mkString(", "))
    // println("  - consumes: " + Util.toArray(op.getConsumes()).mkString(", "))
    // println("  - produces: " + Util.toArray(op.getProduces).mkString(", "))
    // getSecurity
    // getVendorExtensions
    // getOperationId (this is like a nick name for the method - e.g. findPets)
    Operation(
      method = method,
      path = Converters.substitutePathParameters(url),
      description = Converters.combine(Seq(summary, description, externalDocsToString(Option(op.getExternalDocs)))),
      deprecation = Option(op.isDeprecated).getOrElse(false) match {
        case false => None
        case true => Some(Deprecation())
      },
      body = bodies.toList match {
        case Nil => None
        case body :: Nil => Some(body)
        case multiple => {
          sys.error("Multiple body parameters specified for operation at url[$url]")
        }
      },
      parameters = parameters,
      responses = responses.toSeq
    )
  }

  private def toSchemaTypeFromStrings(
    resolver: Resolver,
    t: String,
    format: Option[String],
    itemProperty: Option[Property]
  ): String = {
    t match {
      case "array" => {
        resolver.schemaType(
          itemProperty.getOrElse {
            sys.error("Need item property for array")
          }
        )
      }
      case _ => {
        translators.SchemaType.fromSwaggerWithError(t, format)
      }
    }
  }

  private def toParameter(
    resolver: Resolver,
    param: com.wordnik.swagger.models.parameters.Parameter
  ): Parameter = {
    // getAccess
    // getVendorExtensions

    val location = ParameterLocation.fromString(param.getIn).getOrElse {
      sys.error(s"Could not translate param[${param.getName}] location[${param.getIn}]")
    }

    val template = Parameter(
      name = param.getName(),
      `type` = "string",
      location = location,
      description = Option(param.getDescription()),
      required = param.getRequired(),
      default = None,
      minimum = None,
      maximum = None,
      example = None
    )

    param match {
      case p: swaggerparams.BodyParameter => {
        sys.error("Should never see body parameter here")
      }
      case p: swaggerparams.QueryParameter => {
        template.copy(`type` = toSchemaTypeFromStrings(resolver, p.getType, Option(p.getFormat), Option(p.getItems)))
      }
      case _ => {
        template
      }
    }
  }

  private def findModelByUrl(
    models: Seq[Model],
    url: String
  ): Option[Model] = {
    val normalized = Converters.normalizeUrl(url)
    models.find { m =>
      val modelUrl = Converters.normalizeUrl(s"/${m.plural}")
      normalized == modelUrl || normalized.startsWith(modelUrl + "/")
    }
  }

}
