package me.apidoc.swagger

import lib.{ServiceConfiguration, Text, UrlKey}
import java.io.File
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets
import scala.collection.JavaConversions._
import play.api.libs.json.{Json, JsArray, JsObject, JsString, JsValue}
import java.util.UUID

import io.swagger.parser.SwaggerParser
import com.wordnik.swagger.models.{ModelImpl, Swagger}
import com.wordnik.swagger.models.properties.{AbstractNumericProperty, ArrayProperty, Property, RefProperty, StringProperty, UUIDProperty}
import com.gilt.apidoc.spec.v0.models._

import lib.Text
import com.gilt.apidoc.spec.v0.models._

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
      resources = resources(swagger, specModels)
    )
  }

  private def models(swagger: Swagger): Seq[Model] = {
    swagger.getDefinitions().map { d =>
      d match {
        case (name, definition) => {
          definition match {
            case m: ModelImpl => {
              // TODO println("  - type: " + Option(schema.getType()))
              // TODO println("  - discriminator: " + Option(schema.getDiscriminator()))
              // TODO println("  - external docs:")
              Option(m.getExternalDocs()).map { doc =>
                // TODO println("    - url: " + doc.getUrl())
                // TODO println("    - description: " + doc.getDescription())
              }
              println("  - addl properties:")
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
                        base.copy(
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

            case _ => {
              sys.error(s"Unsupported definition for name[$name]: $definition")
            }
          }
        }
      }
    }.toSeq
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
    //printMethods(swagger)

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
    println("  - tags: " + toArray(op.getTags()).mkString(", "))

    val summary = Option(op.getSummary())
    val description = Option(op.getDescription())

    val parameters = toArray(op.getParameters).map { param =>
      toParameter(models, param)
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
    // getOperationId
    Operation(
      method = method,
      path = url,
      description = combine(Seq(summary, description)),
      deprecation = Option(op.isDeprecated).getOrElse(false) match {
        case false => None
        case true => Some(Deprecation())
      },
      //body = ?, TODO
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
        Option(prop.getFormat()) match {
          case None => {
            SchemaType.fromSwagger(prop.getType()).getOrElse {
              sys.error(s"Unrecognized swagger type[${prop.getType()}]: ${prop.getClass}")
            }
          }
          case Some(format) => {
            SchemaType.fromSwagger(format).getOrElse {
              sys.error(s"Unrecognized swagger type[$format]: ${prop.getClass}")
            }
          }
        }
      }
    }
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

    val p = Parameter(
      name = param.getName(),
      `type` = SchemaType.fromSwagger("string").getOrElse { // TODO
        sys.error(s"Unrecognized swagger parameter type[]: param[${param.getName}] class[${param.getClass}]")
      },
      location = location,
      description = Option(param.getDescription()),
      required = param.getRequired(),
      default = None,
      minimum = None,
      maximum = None,
      example = None
    )

    param match {
      case _ => p
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
      `type` = toSchemaType(response.getSchema, models),
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

  private def printMethods(instance: Any) {
    println(s"${instance.getClass()}")
    instance.getClass().getMethods().map(_.toString).sorted.foreach { m =>
      if (m.toString().indexOf("get") >= 0) {
        println("  - method: " + m)
      }
    }
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
