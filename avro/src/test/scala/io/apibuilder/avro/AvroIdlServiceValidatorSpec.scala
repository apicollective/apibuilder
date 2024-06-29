package io.apibuilder.avro

import helpers.ValidatedTestHelpers
import lib.{FileUtils, ServiceConfiguration}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.io.File

class AvroIdlServiceValidatorSpec extends AnyFunSpec with Matchers with ValidatedTestHelpers {

  private val validator: AvroIdlServiceValidator = AvroIdlServiceValidator(
    ServiceConfiguration(
      orgKey = "gilt",
      orgNamespace = "io.apibuilder",
      version = "0.0.1-dev"
    )
  )

  it("parses") {
    expectValid {
      validator.validate(FileUtils.readToString(new File("avro/example.avdl")))
    }
  }

}
