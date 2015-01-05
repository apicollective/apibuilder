package core

import org.scalatest.{FunSpec, Matchers}

class ImportTypeSpec extends FunSpec with Matchers {

  describe("validation") {

    val baseJson = """
    {
      "name": "Import Shared",
      "imports": [
	{ "uri": "%s" }
      ]
    }
  """

    it("import uri is present") {
      val json = """{
        "name": "Import Shared",
        "imports": [ { "foo": "bar" } ]
      }"""
      val validator = ServiceValidator(TestHelper.serviceConfig, json)
      validator.errors.mkString("") should be("imports.uri is required")
    }

    it("import uri cannot be empty") {
      val validator = ServiceValidator(TestHelper.serviceConfig, baseJson.format("  "))
      validator.errors.mkString("") should be("imports.uri is required")
    }

    it("import uri is a URI") {
      val validator = ServiceValidator(TestHelper.serviceConfig, baseJson.format("foobar"))
      validator.errors.mkString("") should be("imports.uri[foobar] is not a valid URI")
    }

  }

  describe("with valid service") {

    val json1 = """
    {
      "name": "Import Shared",
      "namespace": "test.apidoc.import-shared",

      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        }
      }
    }
  """

    val json1File = TestHelper.writeToTempFile(json1)

    val json2 = s"""
    {
      "name": "Import Service",

      "imports": [
	{ "uri": "file://$json1File" }
      ],

      "models": {
        "membership": {
          "fields": [
            { "name": "id", "type": "long" },
            { "name": "user", "type": "test.apidoc.import-shared.user" }
          ]
        }
      }
    }
  """

    it("parses service definition with imports") {
      val validator = ServiceValidator(TestHelper.serviceConfig, json2)
      validator.errors.mkString("") should be("")
    }
  }
}
