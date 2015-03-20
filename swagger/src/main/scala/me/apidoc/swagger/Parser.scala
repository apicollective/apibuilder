package me.apidoc.swagger

import lib.{ServiceConfiguration, UrlKey}
import java.io.File
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets
import scala.collection.JavaConversions._
import play.api.libs.json.{Json, JsArray, JsObject, JsString, JsValue}
import java.util.UUID

import io.swagger.parser.SwaggerParser
import com.wordnik.swagger.models.{ModelImpl, Swagger}

import lib.Text
import com.gilt.apidoc.spec.v0.models._

case class Parser(config: ServiceConfiguration) {

  def parse(
    path: File
  ): Service = {
    val swagger = new SwaggerParser().read(path.toString)
    println(swagger)
    //printMethods(swagger)

    println("swagger version: " + swagger.getSwagger())

    val info = swagger.getInfo()
    println("info:")
    println(" - title: " + info.getTitle())
    println(" - description: " + info.getDescription())
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

    println(" - version: " + info.getVersion())

    println("host: " + swagger.getHost())
    println("basePath: " + swagger.getBasePath())
    println("schemes: " + toArray(swagger.getSchemes()).mkString(", "))
    val baseUrl = swagger.getSchemes.headOption.map(_.toString.toLowerCase).getOrElse("http") + "//" + swagger.getHost() + swagger.getBasePath()
    println("baseUrl: " + baseUrl)

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

    swagger.getDefinitions.foreach { d =>
      d match {
        case (name, definition) => {
          definition match {
            case schema: ModelImpl => {
              //printMethods(schema)

              println(s"definition[$name]: " + schema)
              println("  - type: " + Option(schema.getType()))
              println("  - description: " + Option(schema.getDescription()))
              println("  - example: " + Option(schema.getExample()))
              println("  - discriminator: " + Option(schema.getDiscriminator()))

              val requiredFieldNames: Seq[String] = schema.getRequired()
              println("    - required: " + requiredFieldNames.mkString(", "))

              schema.getProperties().foreach {
                case (key, prop) => {
                  println(s"  - prop[$key]")
                  
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

                  println(s"    - apidoc type: " + schemaType.apidoc)
                  println(s"    - title: " + Option(prop.getTitle()))
                  println(s"    - description: " + Option(prop.getDescription()))
                  println(s"    - name: " + Option(prop.getName()))
                  println(s"    - required: " + prop.getRequired())
                  println(s"    - example: " + Option(prop.getExample()))

                  // Ignoring:
                  println(s"    - readOnly: " + Option(prop.getReadOnly()))
                  println(s"    - xml: " + Option(prop.getXml()))
                }
              }

              println("  - external docs:")
              val docs = if (schema.getExternalDocs() == null) { None } else { Some(schema.getExternalDocs()) }
              docs.map { doc =>
                println("    - url: " + doc.getUrl())
                println("    - description: " + doc.getDescription())
              }

              println("  - addl properties:")
              Option(schema.getAdditionalProperties()) match {
                case None => {
                  println("    - None")
                }
                case Some(property) => {
                  println("    - " + property)
                }
              }
            }
            case _ => {
              sys.error(s"Unsupported definition: $definition")
            }
          }
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
