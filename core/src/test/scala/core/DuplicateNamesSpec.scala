package core

import org.scalatest.{FunSpec, Matchers}

class DuplicateNamesSpec extends FunSpec with Matchers {

  it("disallow model names when camel case vs. snake case") {
    val json =
      """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "some_user": {
          "fields": [
            { "name": "id", "type": "string" }
          ]
        },
        "someUser": {
          "fields": [
            { "name": "id", "type": "string" }
          ]
        }
      }
    }
    """

    TestHelper.serviceValidatorFromApiJson(json).errors should be(
      Seq("Model[some_user] appears more than once")
    )
  }

  it("disallow enum values when camel case vs. snake case") {
    val json =
      """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "enums": {
        "foo": {
          "values": [
            { "name": "some_id"},
            { "name": "someId" }
          ]
        }
      }
    }
    """

    TestHelper.serviceValidatorFromApiJson(json).errors should be(
      Seq("Enum[foo] value[some_id] appears more than once")
    )
  }

  it("disallow model field names when camel case vs. snake case") {
    val json =
      """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": [
            { "name": "someId", "type": "string" },
            { "name": "some_id", "type": "string" }
          ]
        }
      }
    }
    """

    TestHelper.serviceValidatorFromApiJson(json).errors should be(
      Seq("Model[user] field[some_id] appears more than once")
    )
  }

  it("disallow parameters duplicate param names when camel case vs. snake case") {
    val json =
      """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "string" }
          ]
        }
      },
      "resources": {
        "user": {
          "operations": [
            {
              "method": "GET",
              "path": "/:guid",
              "parameters": [
                { "name": "some_id", "type": "string" },
                { "name": "some_id", "type": "string" }
              ],
              "responses": {
                "204": { "type": "unit" }
              }
            }
          ]
        }
      }
    }
    """

    TestHelper.serviceValidatorFromApiJson(json.format("user")).errors should be(
      Seq("Resource[user] GET /users/:guid Parameter[some_id] appears more than once")
    )
  }
}
