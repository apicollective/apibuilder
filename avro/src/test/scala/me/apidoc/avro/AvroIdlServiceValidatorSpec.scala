package me.apidoc.avro

import lib.ServiceConfiguration
import org.scalatest.{FunSpec, Matchers}

class AvroIdlServiceValidatorSpec extends FunSpec with Matchers {

  private def readFile(path: String): String = {
    scala.io.Source.fromFile(path).getLines.mkString("\n")
  }

  val config = ServiceConfiguration(
    orgKey = "gilt",
    orgNamespace = "com.bryzek",
    version = "0.0.1-dev"
  )

  it("parses") {
    AvroIdlServiceValidator(config, readFile("avro/example.avdl")).validate match {
      case Left(errors) => {
        println("ERRORS: " + errors.mkString(", "))
      }
      case Right(service) => {
        println("No errors. Service: " + service.name)
      }
    }
  }

}
