package core

import builder.OriginalValidator
import io.apibuilder.api.json.v0.models.{ApiJson, Import}
import io.apibuilder.api.v0.models.{Original, OriginalType}
import org.scalatest.{FunSpec, Matchers}

class ImportServiceSpec extends FunSpec with Matchers with helpers.ApiJsonHelpers {

  def buildApiJson(
    imports: Seq[Import] = Nil,
  ): ApiJson = {
    makeApiJson(
      imports = imports,
    )
  }

  describe("validation") {

    it("import uri is present") {
      val json = """{
        "name": "Import Shared",
        "apidoc": { "version": "0.9.6" },
        "info": {},
        "imports": [ { "foo": "bar" } ]
      }"""
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors().mkString(",") should be("Import Unrecognized element[foo],Import Missing uri")
    }

    it("import uri cannot be empty") {
      def testUri(uri: String) = {
        TestHelper.serviceValidator(
          makeApiJson(
            imports = Seq(makeImport(uri = uri))
          )
        )
      }
      testUri("  ").errors should be(Seq("Import uri must be a non empty string"))
      testUri("foobar").errors should be(Seq("URI[foobar] must start with http://, https://, or file://"))
      testUri("https://app.apibuilder.io/").errors should be(Seq("URI[https://app.apibuilder.io/] cannot end with a '/'"))
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
      "namespace": "test.apibuilder.import-shared",
      "version": "1.0.0",
      "attributes": [],

      "imports": [],
      "headers": [],
      "resources": [],
      "interfaces": [],

      "enums": [
        {
          "name": "age_group",
          "plural": "age_groups",
          "values": [
            { "name": "youth", "attributes": [] },
            { "name": "adult", "attributes": [] }
          ],
          "attributes": []
        }
      ],

      "unions": [
        {
          "name": "user_or_guest",
          "plural": "user_or_guests",
          "types": [
            { "type": "user", "attributes": [] },
            { "type": "guest", "attributes": [] }
          ],
          "interfaces": [],
          "attributes": []
        },

        {
          "name": "user_or_random",
          "plural": "user_or_randoms",
          "types": [
            { "type": "user", "attributes": [] },
            { "type": "random_user", "attributes": [] }
          ],
          "interfaces": [],
          "attributes": []
        }
      ],

      "models": [
        {
          "name": "user",
          "plural": "users",
          "fields": [
            { "name": "id", "type": "long", "required": true, "attributes": [] }
          ],
          "interfaces": [],
          "attributes": []
        },

        {
          "name": "guest",
          "plural": "guests",
          "fields": [
            { "name": "id", "type": "long", "required": true, "attributes": [] }
          ],
          "interfaces": [],
          "attributes": []
        },

        {
          "name": "random_user",
          "plural": "random_users",
          "fields": [
            { "name": "id", "type": "uuid", "required": true, "attributes": [] }
          ],
          "interfaces": [],
          "attributes": []
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
          "interfaces": [],
          "fields": [
            { "name": "id", "type": "long" },
            { "name": "user", "type": "test.apibuilder.import-shared.models.user" },
            { "name": "age_group", "type": "test.apibuilder.import-shared.enums.age_group" }
          ]
        }
      },

      "resources": {
        "test.apibuilder.import-shared.models.user": {
          "operations": [
            {
              "method": "GET",
              "path": "/get/:id"
            }
          ]
        },

        "test.apibuilder.import-shared.unions.user_or_guest": {
          "operations": [
            {
              "method": "GET",
              "path": "/get/:id"
            }
          ]
        },

        "test.apibuilder.import-shared.unions.user_or_random": {
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

    lazy val validService = validator.validate match {
      case Left(errors) => sys.error(errors.mkString(","))
      case Right(service) => service
    }

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
      val resource = validService.resources.find(_.`type` == "test.apibuilder.import-shared.models.user").getOrElse {
        sys.error("Could not find resource")
      }
      resource.operations.head.parameters.find(_.name == "id").getOrElse {
        fail("Could not find parameter named[id]")
      }.`type` should be("long")
    }

    it("infers datatype for an imported field from a union type") {
      val resource = validService.resources.find(_.`type` == "test.apibuilder.import-shared.unions.user_or_guest").getOrElse {
        sys.error("Could not find resource")
      }
      resource.operations.head.parameters.find(_.name == "id").getOrElse {
        fail("Could not find parameter named[id]")
      }.`type` should be("long")
    }

    it("defaults datatype to string when type varies across union types") {
      val resource = validService.resources.find(_.`type` == "test.apibuilder.import-shared.unions.user_or_random").getOrElse {
        sys.error("Could not find resource")
      }
      resource.operations.head.parameters.find(_.name == "id").getOrElse {
        fail("Could not find parameter named[id]")
      }.`type` should be("string")
    }

  }
}
