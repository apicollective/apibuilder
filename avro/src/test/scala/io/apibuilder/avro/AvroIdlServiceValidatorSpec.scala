package io.apibuilder.avro

import helpers.ValidatedTestHelpers
import lib.ServiceConfiguration
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class AvroIdlServiceValidatorSpec extends AnyFunSpec with Matchers with ValidatedTestHelpers {

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
    expectValid {
      AvroIdlServiceValidator(config, readFile("avro/example.avdl")).validate()
    }
  }

}
