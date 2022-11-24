package io.apibuilder.avro

import helpers.ValidatedTestHelpers
import lib.{FileUtils, ServiceConfiguration}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.io.File

class AvroIdlServiceValidatorSpec extends AnyFunSpec with Matchers with ValidatedTestHelpers {

  private def readFile(path: String): String = FileUtils.readToString(new File(path))

  val config: ServiceConfiguration = ServiceConfiguration(
    orgKey = "gilt",
    orgNamespace = "io.apibuilder",
    version = "0.0.1-dev"
  )

  it("parses") {
    expectValid {
      AvroIdlServiceValidator(config).validate(readFile("avro/example.avdl"))
    }
  }

}
