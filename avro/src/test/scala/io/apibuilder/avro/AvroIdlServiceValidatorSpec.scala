package io.apibuilder.avro

import lib.ServiceConfiguration
import org.scalatest.{FunSpec, Matchers}

class AvroIdlServiceValidatorSpec extends FunSpec with Matchers {

  private def readFile(path: String): String = {
    val source = scala.io.Source.fromFile(path)
    try {
      source.getLines().mkString("\n")
    } finally {
      source.close()
    }
  }

  val config: ServiceConfiguration = ServiceConfiguration(
    orgKey = "gilt",
    orgNamespace = "io.apibuilder",
    version = "0.0.1-dev"
  )

  it("parses") {
    AvroIdlServiceValidator(config, readFile("avro/example.avdl")).validate() match {
      case Left(errors) => {
        println("ERRORS: " + errors.mkString(", "))
      }
      case Right(service) => {
        println("No errors. Service: " + service.name)
      }
    }
  }

}
