package core

import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ServiceParametersSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  it("fails object type path parameters") {
    val baseJson = """
    {
      "name": "API Builder",

      "models": {
        "user": { "fields": [ { "name": "id", "type": "long" } ] }
      },

      "resources": {
        "user": {
          "path": "/foo/:id",
          "operations": [
            {
              "method": "GET",
              "parameters": [ { "name": "id", "type": "object", "location": "path" } ]
            }
          ]
        }
      }
    }
    """

    val json = baseJson.format("age")
    TestHelper.expectSingleError(json) shouldBe "Resource[user] GET /foo/:id path parameter[id] has an invalid type[object]. Valid types for path parameters are: enum, boolean, decimal, integer, double, long, string, date-iso8601, date-time-iso8601, uuid."
  }

  it("fails unit type path parameters") {
    val baseJson = """
    {
      "name": "API Builder",

      "models": {
        "user": { "fields": [ { "name": "id", "type": "long" } ] }
      },

      "resources": {
        "user": {
          "path": "/foo/:id",
          "operations": [
            {
              "method": "GET",
              "parameters": [ { "name": "id", "type": "unit", "location": "path" } ]
            }
          ]
        }
      }
    }
    """

    val json = baseJson.format("age")
    TestHelper.expectSingleError(json) shouldBe "Resource[user] GET /foo/:id path parameter[id] has an invalid type[unit]. Valid types for path parameters are: enum, boolean, decimal, integer, double, long, string, date-iso8601, date-time-iso8601, uuid."
  }

  it("fails object type query parameters") {
    val baseJson = """
    {
      "name": "API Builder",

      "models": {
        "user": { "fields": [ { "name": "id", "type": "long" } ] }
      },

      "resources": {
        "user": {
          "path": "/foo",
          "operations": [
            {
              "method": "GET",
              "parameters": [ { "name": "id", "type": "object", "location": "query" } ]
            }
          ]
        }
      }
    }
    """

    val json = baseJson.format("age")
    TestHelper.expectSingleError(json) shouldBe "Resource[user] GET /foo Parameter[id] has an invalid type[object]. Valid types for query parameters are: enum, boolean, decimal, integer, double, long, string, date-iso8601, date-time-iso8601, uuid."
  }

  it("fails unit type header parameters") {
    val baseJson = """
    {
      "name": "API Builder",

      "models": {
        "user": { "fields": [ { "name": "id", "type": "long" } ] }
      },

      "resources": {
        "user": {
          "path": "/foo",
          "operations": [
            {
              "method": "GET",
              "parameters": [ { "name": "id", "type": "unit", "location": "header" } ]
            }
          ]
        }
      }
    }
    """

    val json = baseJson.format("age")
    TestHelper.expectSingleError(json) shouldBe "Resource[user] GET /foo Parameter[id] has an invalid type[unit]. Valid types for header parameters are: enum, boolean, decimal, integer, double, long, string, date-iso8601, date-time-iso8601, uuid."
  }

  it("fails [object] type query parameters") {
    val baseJson = """
    {
      "name": "API Builder",

      "models": {
        "user": { "fields": [ { "name": "id", "type": "long" } ] }
      },

      "resources": {
        "user": {
          "path": "/foo",
          "operations": [
            {
              "method": "GET",
              "parameters": [ { "name": "id", "type": "[object]", "location": "query" } ]
            }
          ]
        }
      }
    }
    """

    val json = baseJson.format("age")
    TestHelper.expectSingleError(json) shouldBe "Resource[user] GET /foo Parameter[id] has an invalid type[[object]]. Valid nested types for lists in query parameters are: enum, boolean, decimal, integer, double, long, string, date-iso8601, date-time-iso8601, uuid."
  }
}
