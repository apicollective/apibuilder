package core

import builder.OriginalValidator
import com.bryzek.apidoc.api.v0.models.{Original, OriginalType}
import org.scalatest.{FunSpec, Matchers}

class ImportServiceSpec extends FunSpec with Matchers {

  describe("validation") {

    val baseJson = """
    {
      "name": "Import Shared",
      "apidoc": { "version": "0.9.6" },
      "info": {},
      "imports": [
	{ "uri": "%s" }
      ]
    }
  """

    it("import uri is present") {
      val json = """{
        "name": "Import Shared",
        "apidoc": { "version": "0.9.6" },
        "info": {},
        "imports": [ { "foo": "bar" } ]
      }"""
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString(",") should be("Import Unrecognized element[foo],Import Missing uri")
    }

    it("import uri cannot be empty") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("  "))
      validator.errors.mkString("") should be("Import uri must be a non empty string")
    }

    it("import uri starts with a valid protocol") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("foobar"))
      validator.errors.mkString("") should be("URI[foobar] must start with http://, https://, or file://")
    }

    it("import uri does not end with a /") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("http://www.apidoc.me/"))
      validator.errors.mkString("") should be("URI[http://www.apidoc.me/] cannot end with a '/'")
    }

  }

  describe("with valid service") {

    val json1 = """
    {
      "name": "Import Shared",
      "apidoc": { "version": "0.9.6" },
      "info": {},
      "organization": { "key": "test" },
      "application": { "key": "import-shared" },
      "namespace": "test.apidoc.import-shared",
      "version": "1.0.0",

      "imports": [],
      "headers": [],
      "unions": [],
      "resources": [],

      "enums": [
        {
          "name": "age_group",
          "plural": "age_groups",
          "values": [
            { "name": "youth" },
            { "name": "adult" }
          ]
        }
      ],

      "models": [
        {
          "name": "user",
          "plural": "users",
          "fields": [
            { "name": "id", "type": "long", "required": true }
          ]
        }
      ]
    }
  """

    val json1File = TestHelper.writeToTempFile(json1)

    val json2 = s"""
    {
      "name": "Import Service",
      "apidoc": { "version": "0.9.6" },
      "info": {},

      "imports": [
	{ "uri": "file://$json1File" }
      ],

      "models": {
        "membership": {
          "fields": [
            { "name": "id", "type": "long" },
            { "name": "user", "type": "test.apidoc.import-shared.models.user" },
            { "name": "age_group", "type": "test.apidoc.import-shared.enums.age_group" }
          ]
        }
      },

      "resources": {
        "test.apidoc.import-shared.models.user": {
          "operations": [
            {
              "method": "GET",
              "path": "/get/:id"
            }
          ]
        }
      }
    }
  """

    lazy val validator = OriginalValidator(
      config = TestHelper.serviceConfig,
      original = Original(OriginalType.ApiJson, json2),
      fetcher = FileServiceFetcher()
    )

    it("parses service definition with imports") {
      validator.validate match {
        case Left(errors) => {
          fail(errors.mkString(""))
        }
        case Right(_) => {
          // Success
        }
      }
    }

    it("infers datatype for an imported field") {
      validator.validate match {
        case Left(errors) => sys.error(errors.mkString(","))
        case Right(service) => {
          val op = service.resources.head.operations.head
          val id = op.parameters.find(_.name == "id").getOrElse {
            fail("Could not find parameter named[id]")
          }
          id.`type` should be("long")
        }
      }
    }

  }
}
