package me.apidoc.swagger

import lib.ServiceConfiguration
import org.scalatest.{FunSpec, Matchers}

class SwaggerServiceValidatorSpec extends FunSpec with Matchers {

  private def readFile(path: String): String = {
    scala.io.Source.fromFile(path).getLines.mkString("\n")
  }

  val config = ServiceConfiguration(
    orgKey = "gilt",
    orgNamespace = "com.gilt",
    version = "0.0.1-dev"
  )

  it("parses") {
    SwaggerServiceValidator(config, readFile("swagger/example-petstore.json")).validate match {
      case Left(errors) => {
        println("ERRORS: " + errors.mkString(", "))
      }
      case Right(service) => {
        println("No errors.")
        println("Service: " + service.name)
        service.models.foreach { m =>
          println(s" Model ${m.name}")
          m.fields.foreach { f =>
            println(s"   - Field ${f.name}, type: ${f.`type`}")
          }
        }

        service.resources.foreach { r =>
          println(s" Resource ${r.`type`}")
          r.operations.foreach { op =>
            println(s"  ${op.method} ${op.path}")
            println(s"   body: " + op.body.map(_.`type`))

            println(s"   parameters:")
            op.parameters match {
              case Nil => {
                println("    none")
              }
              case params => {
                params.foreach { p =>
                  println(s"    ${p.name}: ${p.`type`}")
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
