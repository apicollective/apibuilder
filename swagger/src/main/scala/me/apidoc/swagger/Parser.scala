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
import com.wordnik.swagger.models.properties.Property
import com.gilt.apidoc.spec.v0.models._

import lib.Text
import com.gilt.apidoc.spec.v0.models._

case class Parser(config: ServiceConfiguration) {

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
      models = models(swagger),
      imports = Nil,
      headers = Nil,
      resources = Nil
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
                    field(key, prop)
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
    val schemaType = Option(prop.getFormat()) match {
      case None => {
        SchemaType.fromSwagger(prop.getType()).getOrElse {
          sys.error(s"Unrecognized swagger type[${prop.getType()}]")
        }
      }
      case Some(format) => {
        SchemaType.fromSwagger(format).getOrElse {
          sys.error(s"Unrecognized swagger type[$format]")
        }
      }
    }

    // Ignoring:
    // println(s"    - readOnly: " + Option(prop.getReadOnly()))
    // println(s"    - xml: " + Option(prop.getXml()))


    Field(
      name = name,
      description = Option(prop.getDescription()),
      `type` = schemaType.apidoc,
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

    swagger.getPaths.foreach {
      case (url, p) => {
        println("path: " + url)
        p.getOperations().foreach { op =>
          println("  - op: " + op)
        }
      }
    }


    // We're at 'path' - https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md

    sys.error("TODO")
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
