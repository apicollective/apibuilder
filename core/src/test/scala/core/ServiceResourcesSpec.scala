package core

import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ServiceResourcesSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  describe("with service") {
    val baseJson = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "enums": {
        "user_type": {
          "values": [
            { "name": "registered" },
            { "name": "guest" }
          ]
        }
      },

      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "long" },
            { "name": "type", "type": "user_type" }
          ]
        }
      },

      "resources": {
        "%s": {
          "operations": [
            {
              "method": "GET",
              "path": "/:id"
            }
          ]
        }
      }
    }
  """

    def setupPath(value: String) = {
      val json = baseJson.format(value)
      setupValidApiJson(json).resources.head.operations.head.path
    }

    def setupInvalidPath(value: String) = {
      TestHelper.expectSingleError(baseJson.format(value))
    }

    it("models can be resources, with valid paths") {
      setupPath("user") should be ("/users/:id")
    }

    it("enums can be resources, with valid paths") {
      setupPath("user_type") should be ("/user_types/:id")
    }

    it("lists cannot be resources") {
      setupInvalidPath("[user]") should be("Resource[[user]] has an invalid type: must be a singleton (not a list nor map)")
    }

    it("maps cannot be resources") {
      setupInvalidPath("[user]") should be("Resource[[user]] has an invalid type: must be a singleton (not a list nor map)")
    }

  }

  it("allows empty path for resource") {
    val json = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        }
      },

      "resources": {
        "user": {
          "path": "",

          "operations": [
            {
              "method": "GET"
            },
            {
              "method": "GET",
              "path": "/foo"
            }
          ]
        }
      }
    }
    """

    val operations = setupValidApiJson(json).resources.head.operations
    operations.head.path should be("/")
    operations.last.path should be("/foo")
  }

  describe("resource types") {
    val baseJson = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "enums": {
        "user_type": {
          "values": [
            { "name": "registered" },
            { "name": "guest" }
          ]
        }
      },

      "resources": {
        "%s": {
          "operations": [
            {
              "method": "GET",
              "path": "/:id"
            }
          ]
        }
      }
    }
    """

    it("validates that resource types are well defined") {
      TestHelper.expectSingleError(baseJson.format("user")) should be("Resource type[user] not found")
    }

    it("enums can be mapped to resources") {
      TestHelper.expectSingleError(baseJson.format("user_type")) should be("")
    }

  }

}
