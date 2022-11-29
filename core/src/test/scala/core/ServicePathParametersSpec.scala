package core

import helpers.ApiJsonHelpers
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class ServicePathParametersSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  describe("with a service") {
    val baseJson = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "enums": {
        "age_group": {
          "values": [
            { "name": "Youth" },
            { "name": "Adult" }
          ]
        }
      },

      "models": {
        "tag": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        },

        "user": {
          "fields": [
            { "name": "id", "type": "long" },
            { "name": "name", "type": "string" },
            { "name": "created_at_date", "type": "date-iso8601" },
            { "name": "created_at_date_time", "type": "date-time-iso8601" },
            { "name": "tag", "type": "tag" },
            { "name": "tags", "type": "map" },
            { "name": "age_group", "type": "age_group" }
          ]
        }
      },

      "resources": {
        "user": {
          "operations": [
            {
              "method": "%s",
              "path": "%s"
            }
          ]
        }
      }
    }
    """

    it("numbers can be path parameters") {
      val json = baseJson.format("GET", "/:id")
      setupValidApiJson(json)
    }

    it("strings can be path parameters") {
      val json = baseJson.format("GET", "/:name")
      setupValidApiJson(json)
    }

    it("supports file extensions") {
      val json = baseJson.format("GET", "/:id.html")
      setupValidApiJson(json)
    }

    it("parameters not defined on the model are accepted (assumed strings)") {
      val json = baseJson.format("GET", "/:some_string")
      setupValidApiJson(json)
    }

    it("enums can be path parameters - assumed type is string") {
      val json = baseJson.format("GET", "/:age_group")
      setupValidApiJson(json)
    }

    it("dates can be path parameters") {
      val json = baseJson.format("GET", "/:created_at_date")
      setupValidApiJson(json)
    }

    it("date-time can be path parameters") {
      val json = baseJson.format("GET", "/:created_at_date_time")
      setupValidApiJson(json)
    }

    it("other models cannot be path parameters") {
      val json = baseJson.format("GET", "/:tag")
      TestHelper.expectSingleError(json) should be(
        "Resource[user] GET /users/:tag path parameter[tag] has an invalid type[tag]. Valid types for path parameters are: enum, boolean, decimal, integer, double, long, string, date-iso8601, date-time-iso8601, uuid."
      )
    }

    it("unsupported types declared as parameters are validated") {
      val json = baseJson.format("POST", "/:tags")
      TestHelper.expectSingleError(json) should be(
        "Resource[user] POST /users/:tags path parameter[tags] has an invalid type[map[string]]. Valid types for path parameters are: enum, boolean, decimal, integer, double, long, string, date-iso8601, date-time-iso8601, uuid."
      )
    }

  }

  describe("w/ a union resource") {

    val baseJson = s"""
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "unions": {
        "user": {
          "types": [
            { "type": "registered" },
            { "type": "guest" }
          ]
        }
      },

      "models": {
        "registered": {
          "fields": [
            { "name": "guid", "type": "uuid" },
            { "name": "age", "type": "integer" }
          ]
        },
        "guest": {
          "fields": [
            { "name": "guid", "type": "uuid" },
            { "name": "age", "type": "long" }
          ]
        }
      },

      "resources": {
        "user": {
          "operations": [
            { "method": "GET", "path": "/users/:%s" }
          ]
        }
      }
    }
    """

    it("can identify common type for path parameter if all union types have the same type") {
      val json = baseJson.format("guid")
      val userResource = setupValidApiJson(json).resources.head
      val op = userResource.operations.head
      val param = op.parameters.head
      param.name should be("guid")
      param.`type` should be("uuid")
    }

    it("uses default 'string' if path parameter type varies across union type") {
      val json = baseJson.format("age")
      val userResource = setupValidApiJson(json).resources.head
      val op = userResource.operations.head
      val param = op.parameters.head
      param.name should be("age")
      param.`type` should be("string")
    }

  }

  it("passes correctly specified path parameters") {
    val baseJson = """
    {
      "name": "API Builder",

      "models": {
        "user": { "fields": [ { "name": "id", "type": "long" } ] }
      },

      "resources": {
        "user": {
          "path": "/foo/:id/bar",
          "operations": [
            {
              "method": "GET",
              "parameters": [ { "name": "id", "type": "long", "location": "path" } ]
            }
          ]
        }
      }
    }
    """

    val json = baseJson.format("age")
    setupValidApiJson(json)
  }

  it("fails missing path parameters") {
    val baseJson = """
    {
      "name": "API Builder",

      "models": {
        "user": { "fields": [ { "name": "id", "type": "long" } ] }
      },

      "resources": {
        "user": {
          "operations": [
            {
              "method": "GET",
              "parameters": [ { "name": "id", "type": "long", "location": "path" } ]
            }
          ]
        }
      }
    }
    """

    val json = baseJson.format("age")
    TestHelper.expectSingleError(json) shouldBe "Resource[user] GET /users path parameter[id] is missing from the path[/users]"
  }

  it("fails incorrectly named path parameters in the middle of the path") {
    val baseJson = """
    {
      "name": "API Builder",

      "models": {
        "user": { "fields": [ { "name": "id", "type": "long" } ] }
      },

      "resources": {
        "user": {
          "path": "/foo/:ids/bar",
          "operations": [
            {
              "method": "GET",
              "parameters": [ { "name": "id", "type": "long", "location": "path" } ]
            }
          ]
        }
      }
    }
    """

    val json = baseJson.format("age")
    TestHelper.expectSingleError(json) shouldBe "Resource[user] GET /foo/:ids/bar path parameter[id] is missing from the path[/foo/:ids/bar]"
  }

  it("fails incorrectly named path parameters at the end of the path") {
    val baseJson = """
    {
      "name": "API Builder",

      "models": {
        "user": { "fields": [ { "name": "id", "type": "long" } ] }
      },

      "resources": {
        "user": {
          "path": "/foo/:ids",
          "operations": [
            {
              "method": "GET",
              "parameters": [ { "name": "id", "type": "long", "location": "path" } ]
            }
          ]
        }
      }
    }
    """

    val json = baseJson.format("age")
    TestHelper.expectSingleError(json) shouldBe "Resource[user] GET /foo/:ids path parameter[id] is missing from the path[/foo/:ids]"
  }
}
