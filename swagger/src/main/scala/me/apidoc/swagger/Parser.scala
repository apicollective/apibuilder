package me.apidoc.swagger

import lib.{ServiceConfiguration, Text, UrlKey}
import java.io.File
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets
import scala.collection.JavaConversions._
import play.api.libs.json.{Json, JsArray, JsObject, JsString, JsValue}
import java.util.UUID

import io.swagger.parser.SwaggerParser
import com.wordnik.swagger.models.{ComposedModel, ModelImpl, RefModel, Swagger}
import com.wordnik.swagger.models.{parameters => swaggerparams}
import com.wordnik.swagger.models.properties.{AbstractNumericProperty, ArrayProperty, Property, RefProperty, StringProperty, UUIDProperty}
import com.gilt.apidoc.spec.v0.models._

import lib.Text
import com.gilt.apidoc.spec.v0.models._
import scala.annotation.tailrec

object Parser {

  private val PathParams = """\{(.+)\}""".r

  def substitutePathParameters(url: String): String = {
    PathParams.replaceAllIn(url, m => ":" + m.group(1))
  }

}

case class Parser(config: ServiceConfiguration) {

  private val DefaultResponseCode = "default"

  def parse(
    path: File
  ): Service = {
    val config = ServiceConfiguration(
      orgKey = "gilt",
      orgNamespace = "com.gilt",
      version = "0.0.1-dev"
    )

    val swagger = new SwaggerParser().read(path.toString)
    val info = swagger.getInfo()
    val applicationKey = UrlKey.generate(info.getTitle())
    val specModels = models(swagger)

    Service(
      name = info.getTitle(),
      description = Option(info.getDescription()),
      baseUrl = Some(swagger.getSchemes.headOption.map(_.toString.toLowerCase).getOrElse("http") + "//" + swagger.getHost() + swagger.getBasePath()),
      namespace = config.applicationNamespace(applicationKey),
      organization = Organization(key = config.orgKey),
      application = Application(key = applicationKey),
      version = info.getVersion(),
      enums = Nil,
      unions = Nil,
      models = specModels,
      imports = Nil,
      headers = Nil,
      resources = mergeResources(resources(swagger, specModels))
    )
  }

  private case class MyDefinition(name: String, definition: com.wordnik.swagger.models.Model)

  private def models(swagger: Swagger): Seq[Model] = {
    buildModels(
      definitions = swagger.getDefinitions.map { case (name, definition) => MyDefinition(name, definition) }.toSeq,
      models = Nil
    )
  }

  private def nextDefinition(
    definitions: Seq[MyDefinition],
    models: Seq[Model]
  ): Option[MyDefinition] = {
    definitions.headOption // TODO
  }

  @tailrec
  private def buildModels(
    definitions: Seq[MyDefinition],
    models: Seq[Model],
    numIterations: Int = 0
  ): Seq[Model] = {
    nextDefinition(definitions, models) match {
      case None => {
        definitions.toList match {
          case Nil => models
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
                case rm: RefModel => resolveRefModel(models, rm)
                case m: ModelImpl => toModel(name, m)
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

          case rm: RefModel => resolveRefModel(models, rm)
          case m: ModelImpl => toModel(name, m)
          case _ => sys.error(s"Unsupported definition for name[$name]")
        }

        buildModels(
          definitions.filter(_.name != name),
          models ++ Seq(newModel),
          numIterations = numIterations + 1
        )
      }
    }
  }


  private def resolveRefModel(
    models: Seq[Model],
    rm: RefModel
  ): Model = {
    // Lookup reference. need to make method iterative
    val name = rm.getSimpleRef()
    models.find(_.name == name).getOrElse {
      sys.error(s"Failed to find a model with name[$name]")
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
      description = choose(r1.description, r2.description),
      deprecation = choose(r1.deprecation, r2.deprecation),
      operations = r1.operations ++ r2.operations
    )
  }

  private def composeModels(m1: Model, m2: Model): Model = {
    m1.copy(
      description = choose(m2.description, m1.description),
      deprecation = choose(m2.deprecation, m1.deprecation),
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
      description = choose(f2.description, f1.description),
      deprecation = choose(f2.deprecation, f1.deprecation),
      default = choose(f2.default, f1.default),
      required = f2.required,
      minimum = choose(f2.minimum, f1.minimum),
      maximum = choose(f2.maximum, f1.maximum),
      example = choose(f2.example, f1.example)
    )
  }

  private def choose[T](a: Option[T], b: Option[T]): Option[T] = {
    a match {
      case None => b
      case Some(_) => a
    }
  }

  private def toModel(
    name: String,
    m: ModelImpl
  ): Model = {
    // TODO println("  - type: " + Option(schema.getType()))
    // TODO println("  - discriminator: " + Option(schema.getDiscriminator()))
    // TODO println("  - external docs:")
    Option(m.getExternalDocs()).map { doc =>
      // TODO println("    - url: " + doc.getUrl())
      // TODO println("    - description: " + doc.getDescription())
    }
    Option(m.getAdditionalProperties()).map { prop =>
      // TODO: println("    - additional property: " + prop)
    }

    Model(
      name = name,
      plural = Text.pluralize(name),
      description = Option(m.getDescription()),
      deprecation = None,
      fields = m.getProperties().map {
        case (key, prop) => {
          val base = field(key, prop)

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
                case Some(pattern) => combine(Seq(base.description, Some(s"Pattern: $pattern")))
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

  private def field(name: String, prop: Property): Field = {
    // Ignoring:
    // println(s"    - readOnly: " + Option(prop.getReadOnly()))
    // println(s"    - xml: " + Option(prop.getXml()))
    Field(
      name = name,
      description = Option(prop.getDescription()),
      `type` = toSchemaType(prop),
      required = prop.getRequired(),
      example = Option(prop.getExample())
    )
  }

  def debug(
    path: File
  ): Service = {
   val swagger = new SwaggerParser().read(path.toString)
    println(swagger)

    println("swagger version: " + swagger.getSwagger())

    val info = swagger.getInfo()
    println("info:")
    println(" - termsOfServiceUrl: " + info.getTermsOfService())

    println(" - contact:")
    Option(info.getContact()) match {
      case None => {
        println("   - none")
      }
      case Some(contact) => {
        println("   - name: " + contact.getName())
        println("   - url: " + contact.getUrl())
        println("   - email: " + contact.getEmail())
      }
    }

    println(" - license:")
    Option(info.getLicense()) match {
      case None => {
        println("   - none")
      }
      case Some(license) => {
        println("   - name: " + license.getName())
        println("   - url: " + license.getUrl())
      }
    }

    println("consumes: " + toArray(swagger.getConsumes()).mkString(", "))
    println("produces: " + toArray(swagger.getProduces).mkString(", "))

    sys.error("TODO")
  }

  private def resources(
    swagger: Swagger,
    models: Seq[Model]
  ): Seq[Resource] = {
    swagger.getPaths.map {
      case (url, p) => {
        val model = findModelByUrl(models, url).getOrElse {
          sys.error(s"Could not find model at url[$url]")
        }

        val operations = Seq(
          Option(p.getGet).map { toOperation(models, Method.Get, url, _) },
          Option(p.getPost).map { toOperation(models, Method.Post, url, _) },
          Option(p.getPut).map { toOperation(models, Method.Put, url, _) },
          Option(p.getDelete).map { toOperation(models, Method.Delete, url, _) },
          Option(p.getOptions).map { toOperation(models, Method.Options, url, _) },
          Option(p.getPatch).map { toOperation(models, Method.Patch, url, _) }
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
    models: Seq[Model],
    method: Method,
    url: String,
    op: com.wordnik.swagger.models.Operation
  ): Operation = {
    // println("  - tags: " + toArray(op.getTags()).mkString(", "))

    val summary = Option(op.getSummary())
    val description = Option(op.getDescription())

    val parameters = toArray(op.getParameters).flatMap { param =>
      param match {
        case p: com.wordnik.swagger.models.parameters.BodyParameter => None
        case _ => Some(toParameter(models, param))
      }
    }

    val bodies = toArray(op.getParameters).flatMap { param =>
      param match {
        case p: com.wordnik.swagger.models.parameters.BodyParameter => Some(toBody(models, p))
        case _ => None
      }
    }

    val responses = op.getResponses.map {
      case (code, swaggerResponse) => {
        toResponse(models, code, swaggerResponse)
      }
    }

    // println("  - schemes: " + toArray(op.getSchemes()).mkString(", "))
    // println("  - consumes: " + toArray(op.getConsumes()).mkString(", "))
    // println("  - produces: " + toArray(op.getProduces).mkString(", "))
    // getSecurity
    // getExternalDocs
    // getVendorExtensions
    // getOperationId (this is like a nick name for the method - e.g. findPets)
    Operation(
      method = method,
      path = Parser.substitutePathParameters(url),
      description = combine(Seq(summary, description)),
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

  private def combine(values: Seq[Option[String]]): Option[String] = {
    values.flatten match {
      case Nil => None
      case v => Some(v.mkString("\n\n"))
    }
  }

  private def toSchemaTypeFromStrings(
    t: String,
    format: Option[String],
    itemProperty: Option[Property],
    models: Seq[Model] = Nil
  ): String = {
    t match {
      case "array" => {
        toSchemaType(
          prop = itemProperty.getOrElse {
            sys.error("Need item property for array")
          },
          models = models
        )
      }
      case _ => {
        SchemaType.fromSwaggerWithError(t, format)
      }
    }
  }

  private def toSchemaType(
    prop: Property,
    models: Seq[Model] = Nil
  ): String = {
    prop match {
      case p: ArrayProperty => {
        val schema = toSchemaType(p.getItems, models)
        val isUnique = Option(p.getUniqueItems) // TODO
        s"[$schema]"
      }
      case p: RefProperty => {
        val model = models.find(_.name == p.getSimpleRef()).getOrElse {
          sys.error("Cannot find model for reference: " + p.get$ref())
        }
        model.name
      }
      case _ => {
        if (prop.getType == null) {
          sys.error(s"Property[${prop}] has no type")
        }
        SchemaType.fromSwaggerWithError(prop.getType, Option(prop.getFormat))
      }
    }
  }

  private def toBody(
    models: Seq[Model],
    param: com.wordnik.swagger.models.parameters.BodyParameter
  ): Body = {
    val bodyType = param.getSchema match {
      case m: ModelImpl => m.getType
      case rm: RefModel => resolveRefModel(models, rm).name
    }

    Body(
      `type` = bodyType,
      description = Option(param.getDescription),
      deprecation = None
    )
  }

  private def toParameter(
    models: Seq[Model],
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
        template.copy(`type` = toSchemaTypeFromStrings(p.getType, Option(p.getFormat), Option(p.getItems), models))
      }
      case _ => {
        template
      }
    }
  }

  private def toResponse(
    models: Seq[Model],
    code: String,
    response: com.wordnik.swagger.models.Response
  ): Response = {
    val intCode = if (code == DefaultResponseCode) {
      409
    } else {
      code.toInt
    }

    // getExamples
    // getHeaders

    Response(
      code = intCode,
      `type` = Option(response.getSchema) match {
        case None => "unit"
        case Some(schema) => toSchemaType(schema, models)
      },
      description = Option(response.getDescription),
      deprecation = None
    )
  }

  private def findModelByUrl(
    models: Seq[Model],
    url: String
  ): Option[Model] = {
    val normalized = normalize(url)
    models.find { m =>
      val modelUrl = normalize(s"/${m.plural}")
      normalized == modelUrl || normalized.startsWith(modelUrl + "/")
    }
  }

  private def normalize(value: String): String = {
    value.toLowerCase.trim.replaceAll("_", "-")
  }

  private def toArray[T](values: java.util.List[T]): Seq[T] = {
    if (values == null) {
      Nil
    } else {
      values
    }
  }

  def parseString(
    contents: String
  ): Service = {
    val tmpPath = "/tmp/apidoc.swagger.tmp." + UUID.randomUUID.toString + ".json"
    writeToFile(tmpPath, contents)
    parse(new File(tmpPath))
  }

  private def writeToFile(path: String, contents: String) {
    val outputPath = Paths.get(path)
    val bytes = contents.getBytes(StandardCharsets.UTF_8)
    Files.write(outputPath, bytes)
  }

}
