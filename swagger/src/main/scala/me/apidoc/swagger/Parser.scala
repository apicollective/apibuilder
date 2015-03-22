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
    swagger.getPaths.foreach {
      case (url, p) => {
        println("url: " + url)

        val model = findModelByUrl(models, url)
        model match {
          case None => {
            println("  - warnings: No model matches path")
          }
          case Some(m) => {
            println("  - model: " + m.name)
          }
        }

        p.getOperations().foreach { op =>
          println("  - tags: " + toArray(op.getTags()).mkString(", "))
          println("  - summary: " + Option(op.getSummary()))
          println("  - description: " + Option(op.getDescription()))
          println("  - schemes: " + toArray(op.getSchemes()).mkString(", "))
          println("  - consumes: " + toArray(op.getConsumes()).mkString(", "))
          println("  - produces: " + toArray(op.getProduces).mkString(", "))

          println("  - parameters:")
          toArray(op.getParameters).foreach { param =>
            println("    - name: " + param.getName())
          }

          println("  - responses:")
          op.getResponses.foreach {
            case (code, swaggerResponse) => {
              val response = toResponse(code, swaggerResponse)
              println("    - code: " + code + " -- TODO")
              println("    - response: " + response)
            }
          }

          val deprecation = Option(op.isDeprecated).getOrElse(false) match {
            case false => None
            case true => Some(Deprecation())
          }
          println("  - deprecation: " + deprecation)

          // getSecurity
          // getExternalDocs
          // getVendorExtensions
        }
      }
    }

    Nil
  }


  private def toSchemaType(prop: Property): String = {
    prop match {
      case p: ArrayProperty => {
        val schema = toSchemaType(p.getItems)
        val isUnique = Option(p.getUniqueItems) // TODO
        "[${schema}]"
      }
      case p: RefProperty => {
        sys.error("TODO: Help!")
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

  private def toResponse(
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
      `type` = toSchemaType(response.getSchema),
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
