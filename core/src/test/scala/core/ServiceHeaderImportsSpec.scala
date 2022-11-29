package core

import helpers.ValidatedTestHelpers
import io.apibuilder.spec.v0.models.Service
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ServiceHeaderImportsSpec extends AnyFunSpec with Matchers with ValidatedTestHelpers {

  private[this] def setupValid(json: String): Service = {
    expectValid {
      TestHelper.serviceValidatorFromApiJson(json)
    }
  }

  describe("valid service") {
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

      "enums": [
        {
          "name": "header_type",
          "plural": "header_types",
          "values": [
            { "name": "xml", "attributes": [] }
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
          "attributes": []
        },

        {
          "name": "user_or_random",
          "plural": "user_or_randoms",
          "types": [
            { "type": "user", "attributes": [] },
            { "type": "random_user", "attributes": [] }
          ],
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
          "attributes": []
        },

        {
          "name": "guest",
          "plural": "guests",
          "fields": [
            { "name": "id", "type": "long", "required": true, "attributes": [] }
          ],
          "attributes": []
        },

        {
          "name": "random_user",
          "plural": "random_users",
          "fields": [
            { "name": "id", "type": "uuid", "required": true, "attributes": [] }
          ],
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

    val baseJson = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "headers": [
        { "name": "Content-Type", "type": "content_type" },
        { "name": "X-Foo", "type": "string", "description": "test", "default": "bar" },
        { "name": "X-Bar", "type": "string", "required": false },
        { "name": "X-Multi", "type": "[string]" }
      ],

      "enums": {
        "content_type": {
          "values": [
            { "name": "application_json" },
            { "name": "application_xml" }
          ]
        }
      }
    }
  """

    it("parses headers") {
      val service = setupValid(baseJson)
      val ctEnum = service.enums.find(_.name == "content_type").get

      val ct = service.headers.find(_.name == "Content-Type").get
      ct.name should be("Content-Type")
      ct.`type` should be("content_type")
      ct.default should be(None)
      ct.required should be(true)
      ct.description should be(None)
      ct.required should be(true)

      val foo = service.headers.find(_.name == "X-Foo").get
      foo.name should be("X-Foo")
      foo.`type` should be("string")
      foo.default should be(Some("bar"))
      foo.description should be(Some("test"))
      foo.required should be(true)

      val bar = service.headers.find(_.name == "X-Bar").get
      bar.name should be("X-Bar")
      bar.`type` should be("string")
      bar.default should be(None)
      bar.description should be(None)
      bar.required should be(false)

      val multi = service.headers.find(_.name == "X-Multi").get
      multi.name should be("X-Multi")
      multi.`type` should be("[string]")
      multi.default should be(None)
      multi.description should be(None)
      multi.required should be(true)
    }
  }

  describe("validation of headers") {
    val baseJson = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "headers": [
        %s
      ]
    }
  """

    it("requires name") {
      val json = baseJson.format("""{ "type": "string" }""")
      TestHelper.expectSingleError(json) should be("Header[] Missing name")
    }

    it("requires type") {
      val json = baseJson.format("""{ "name": "no_type" }""")
      TestHelper.expectSingleError(json) should be("Header[no_type] Missing type")
    }

    it("validates type") {
      val json = baseJson.format("""{ "name": "invalid_type", "type": "integer" }""")
      TestHelper.expectSingleError(json) should be("Header[invalid_type] type[integer] is invalid: Must be a string or the name of an enum")
    }

    it("validates duplicates") {
      val json = baseJson.format("""{ "name": "dup", "type": "string" }, { "name": "dup", "type": "string" }""")
      TestHelper.expectSingleError(json) should be("Header[dup] appears more than once")
    }

  }

}
