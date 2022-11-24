package core

import _root_.builder.OriginalValidator
import helpers.ValidatedTestHelpers
import io.apibuilder.api.v0.models.OriginalType.Swagger
import lib.{FileUtils, ServiceConfiguration}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class OriginalValidatorSpec
  extends AnyFunSpec
  with Matchers
  with ValidatedTestHelpers
{

  private def readFile(path: String): String = FileUtils.readToString(path)

  private val config: ServiceConfiguration = ServiceConfiguration(
    orgKey = "apidoc",
    orgNamespace = "me.apidoc",
    version = "0.0.2-dev"
  )
  private val validator = OriginalValidator(
    config,
    `type` = Swagger,
    MockServiceFetcher()
  )

  describe("OriginalValidator") {

    it("should validate valid swagger json with parameter of type array") {
      val filename = "simple-w-array.json"
      val path = s"core/src/test/resources/$filename"
      expectValid {
        validator.validate(readFile(path))
      }
    }

    it("should validate valid swagger json without parameter of type array") {
      val filename = "simple-without-array.json"
      val path = s"core/src/test/resources/$filename"
      expectValid {
        validator.validate(readFile(path))
      }
    }
  }
}
