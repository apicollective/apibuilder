package core

import helpers.ApiJsonHelpers
import io.apibuilder.spec.v0.models.Method
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class DeprecationSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  private val userModel = """{ "fields": [{ "name": "id", "type": "long" }] }"""

  it("enum") {
    val json = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "enums": {
        "old_content_type": {
          "deprecation": { "description": "blah" },
          "values": [
            { "name": "application_json" },
            { "name": "application_xml" }
          ]
        },

        "content_type": {
          "values": [
            { "name": "application_json" },
            { "name": "application_xml" }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)
    service.enums.find(_.name == "old_content_type").get.deprecation.flatMap(_.description) should be(Some("blah"))
    service.enums.find(_.name == "content_type").get.deprecation.flatMap(_.description) should be(None)
  }

  it("enum value") {
    val json = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "enums": {
        "content_type": {
          "values": [
            { "name": "application_json", "deprecation": { "description": "blah" } },
            { "name": "application_xml" }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)
    val ct = service.enums.find(_.name == "content_type").get

    ct.values.find(_.name == "application_json").get.deprecation.flatMap(_.description) should be(Some("blah"))
    ct.values.find(_.name == "application_xml").get.deprecation.flatMap(_.description) should be(None)
  }

  it("union") {
    val json = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "unions": {
        "old_content_type": {
          "deprecation": { "description": "blah" },
          "types": [
            { "type": "api_json" },
            { "type": "avro_idl" }
          ]
        },

        "content_type": {
          "types": [
            { "type": "api_json" },
            { "type": "avro_idl" }
          ]
        }
      },

      "models": {

        "api_json": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        },

        "avro_idl": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        }

      }
    }
    """

    val service = setupValidApiJson(json)
    service.unions.find(_.name == "old_content_type").get.deprecation.flatMap(_.description) should be(Some("blah"))
    service.unions.find(_.name == "content_type").get.deprecation.flatMap(_.description) should be(None)
  }

  it("union type") {
    val json = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "unions": {
        "content_type": {
          "types": [
            { "type": "api_json", "deprecation": { "description": "blah" } },
            { "type": "avro_idl" }
          ]
        }
      },

      "models": {

        "api_json": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        },

        "avro_idl": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        }

      }
    }
    """

    val service = setupValidApiJson(json)
    val union = service.unions.find(_.name == "content_type").get
    union.types.find(_.`type` == "api_json").get.deprecation.flatMap(_.description) should be(Some("blah"))
    union.types.find(_.`type` == "avro_idl").get.deprecation.flatMap(_.description) should be(None)
  }

  it("model") {
    val json = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "models": {

        "api_json": {
          "deprecation": { "description": "blah" },
          "fields": [
            { "name": "id", "type": "long" }
          ]
        },

        "avro_idl": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        }

      }
    }
    """

    val service = setupValidApiJson(json)
    service.models.find(_.name == "api_json").get.deprecation.flatMap(_.description) should be(Some("blah"))
    service.models.find(_.name == "avro_idl").get.deprecation.flatMap(_.description) should be(None)
  }

  it("field") {
    val json = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "models": {

        "user": {
          "fields": [
            { "name": "id", "type": "long" },
            { "name": "email", "type": "string", "deprecation": { "description": "blah" } }
          ]
        }

      }
    }
    """

    val service = setupValidApiJson(json)
    val user = service.models.find(_.name == "user").get
    user.fields.find(_.name == "id").get.deprecation.flatMap(_.description) should be(None)
    user.fields.find(_.name == "email").get.deprecation.flatMap(_.description) should be(Some("blah"))
  }

  it("resource") {
    val json = s"""
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "models": {
        "user": $userModel,
        "old_user": $userModel
      },

      "resources": {
        "user": {
          "operations": [
            { "method": "GET" }
          ]
        },

        "old_user": {
          "deprecation": { "description": "blah" },
          "operations": [
            { "method": "GET" }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)
    service.resources.find(_.`type` == "user").get.deprecation.flatMap(_.description) should be(None)
    service.resources.find(_.`type` == "old_user").get.deprecation.flatMap(_.description) should be(Some("blah"))
  }

  it("operation") {
    val json = s"""
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "models": {
        "user": $userModel
      },

      "resources": {
        "user": {
          "operations": [
            { "method": "GET" },
            { "method": "DELETE", "deprecation": { "description": "blah" } }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)
    val resource = service.resources.find(_.`type` == "user").get
    resource.operations.find(_.method == Method.Get).get.deprecation.flatMap(_.description) should be(None)
    resource.operations.find(_.method == Method.Delete).get.deprecation.flatMap(_.description) should be(Some("blah"))
  }

  it("parameter") {
    val json = s"""
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "models": {
        "user": $userModel
      },

      "resources": {
        "user": {
          "operations": [
            {
              "method": "GET",
              "parameters": [
                { "name": "id", "type": "long" },
                { "name": "guid", "type": "uuid", "required": false, "deprecation": { "description": "blah" } }
              ]
            }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)

    val resource = service.resources.find(_.`type` == "user").get
    val op = resource.operations.head
    op.parameters.find(_.name == "id").get.deprecation.flatMap(_.description) should be(None)
    op.parameters.find(_.name == "guid").get.deprecation.flatMap(_.description) should be(Some("blah"))
  }

  it("response") {
    val json = s"""
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "models": {
        "user": $userModel
      },

      "resources": {
        "user": {
          "operations": [
            {
              "method": "GET",
              "responses": {
                "200": { "type": "user" },
                "201": { "type": "user", "deprecation": { "description": "blah" } }
              }
            }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)

    val resource = service.resources.find(_.`type` == "user").get
    val op = resource.operations.head
    op.responses.find(r => r.code == "200").get.deprecation.flatMap(_.description) should be(None)
    op.responses.find(r => r.code == "201").get.deprecation.flatMap(_.description) should be(Some("blah"))
  }

  it("body") {
    val json = s"""
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "models": {
        "user": $userModel
      },

      "resources": {
        "user": {
          "operations": [
            {
              "method": "POST",
              "body": { "type": "user" }
            },
            {
              "method": "Put",
              "body": { "type": "user", "deprecation": { "description": "blah" } }
            }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)

    val resource = service.resources.find(_.`type` == "user").get
    resource.operations.find(_.method == Method.Post).get.body.get.deprecation.flatMap(_.description) should be(None)
    resource.operations.find(_.method == Method.Put).get.body.get.deprecation.flatMap(_.description) should be(Some("blah"))
  }

  it("headers") {
    val json = s"""
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "headers": [
        { "name": "Content-Type", "type": "string" },
        { "name": "Old-Content-Type", "type": "string", "deprecation": { "description": "blah" } }
      ]
    }
    """

    val service = setupValidApiJson(json)

    service.headers.find(_.`name` == "Content-Type").get.deprecation.flatMap(_.description) should be(None)
    service.headers.find(_.`name` == "Old-Content-Type").get.deprecation.flatMap(_.description) should be(Some("blah"))
  }


  it("does not require a description") {
    val json = s"""
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "headers": [
        { "name": "Content-Type", "type": "string", "deprecation": {} }
      ]
    }
    """

    val service = setupValidApiJson(json)
    service.headers.find(_.`name` == "Content-Type").get.deprecation.get.description should be(None)
  }

}
