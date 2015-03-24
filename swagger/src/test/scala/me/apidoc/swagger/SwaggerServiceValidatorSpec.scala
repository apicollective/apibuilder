package me.apidoc.swagger

import lib.ServiceConfiguration
import org.scalatest.{FunSpec, Matchers}

class SwaggerServiceValidatorSpec extends FunSpec with Matchers {

  private def readFile(path: String): String = {
    scala.io.Source.fromFile(path).getLines.mkString("\n")
  }

  private def printRequired(value: Boolean): String = {
    value match {
      case true => "(required)"
      case false => "(optional)"
    }
  }

  val config = ServiceConfiguration(
    orgKey = "gilt",
    orgNamespace = "com.gilt",
    version = "0.0.1-dev"
  )

  it("parses") {
    // swagger/example-petstore.json
    SwaggerServiceValidator(config, readFile("swagger/test/resources/petstore-with-external-docs.json")).validate match {
      case Left(errors) => {
        println("ERRORS: " + errors.mkString(", "))
      }
      case Right(service) => {
        println("No errors.")
        println("Service: " + service.name)
        service.models.foreach { m =>
          println(s" Model ${m.name}")
          m.fields.foreach { f =>
            println(s"   - Field ${f.name}, type: ${f.`type`} ${printRequired(f.required)}")
          }
        }

        service.resources.foreach { r =>
          println(s" Resource ${r.`type`}")
          r.operations.foreach { op =>
            println(s"  ${op.method} ${op.path}")

            println(s"   body:")
            op.body match {
              case None => {
                println("    none")
              }
              case Some(b) => {
                println(s"    ${b.`type`}")
              }
            }
              
            println(s"   parameters:")
            op.parameters match {
              case Nil => {
                println("    none")
              }
              case params => {
                params.foreach { p =>
                  println(s"    ${p.name}: ${p.`type`} (${p.location}) ${printRequired(p.required)}")
                }
              }
            }

            println(s"   responses:")
            op.responses.foreach { r =>
              println(s"    ${r.code}: ${r.`type`}")
            }
          }
        }
      }
    }
  }

}
