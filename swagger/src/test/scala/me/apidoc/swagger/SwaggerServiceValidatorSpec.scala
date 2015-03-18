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
        println("No errors. Service: " + service.name)
      }
    }
  }

}
