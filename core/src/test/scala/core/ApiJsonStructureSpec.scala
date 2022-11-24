package core

import helpers.ValidatedTestHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ApiJsonStructureSpec extends AnyFunSpec with Matchers with ValidatedTestHelpers {

  it("name is required") {
    TestHelper.expectSingleError("""
    {
      "apidoc": { "version": "0.9.6" }
    }
    """) shouldBe "Missing name"
  }

  it("name must be a string") {
    TestHelper.expectSingleError(
      """
    {
      "name": { "foo": "bar" },
      "apidoc": { "version": "0.9.6" }
    }
    """) shouldBe "name must be a string"
  }

  it("base_url must be a string") {
    TestHelper.expectSingleError("""
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "base_url": { "foo": "bar" }
    }
    """) should be("base_url, if present, must be a string")
  }

  it("description must be a string") {
    TestHelper.expectSingleError("""
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "description": ["test"]
    }
    """) should be("description, if present, must be a string")
  }

  it("imports, headers must be arrays") {
    val json = """
      {
        "name": "test",
        "apidoc": { "version": "0.9.6" },
        "%s": { "foo": "bar" }
      }
    """

    Seq("imports", "headers").foreach { field =>
      TestHelper.expectSingleError(json.format(field)) should be(s"$field, if present, must be an array")
    }
  }

  it("enums, models, unions, resources must be objects") {
    val json = """
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "%s": []
    }
    """

    Seq("enums", "models", "unions", "resources").foreach { field =>
      TestHelper.expectSingleError(json.format(field)) should be(s"$field, if present, must be an object")
    }
  }

  it("validates unknown keys") {
    TestHelper.expectSingleError("""
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "resource": []
    }
    """) should be("Unrecognized element[resource]")
  }

  it("validates multiple unknown keys") {
    TestHelper.expectSingleError("""
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "resource": [],
      "foo": []
    }
    """) should be("Unrecognized elements[foo, resource]")
  }

  it("validates models") {
    TestHelper.expectSingleError("""
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "description": []
        }
      }
    }
    """) should be("Model[user] description, if present, must be a string")
  }

  it("validates model fields are objects") {
    TestHelper.expectSingleError("""
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": ["test"]
        }
      }
    }
    """) should be("Model[user] elements of fields must be objects")
  }

  it("validates enums") {
    TestHelper.expectSingleError("""
      {
        "name": "test",
        "apidoc": { "version": "0.9.6" },
        "enums": {
          "user": {
            "description": [],
            "values": [{ "name": "test" }]
          }
        }
      }
      """
    ) should be("Enum[user] description, if present, must be a string")
  }

  it("validates enum values are objects") {
    TestHelper.expectSingleError("""
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "enums": {
        "user": {
          "values": ["test"]
        }
      }
    }
    """) should be("Enum[user] elements of values must be objects")
  }

  it("validates unions") {
    expectInvalid {
      TestHelper.serviceValidatorFromApiJson(
        """
        {
          "name": "test",
          "apidoc": { "version": "0.9.6" },
          "unions": {
            "user": {
              "description": []
            }
          }
        }
      """)
    } should be(Seq(
      "Union[user] description, if present, must be a string",
      "Union[user] Missing types"
    ))
  }

  it("validates union types are objects") {
    TestHelper.expectSingleError("""
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "unions": {
        "user": {
          "types": ["test"]
        }
      }
    }
    """) should be("Union[user] elements of types must be objects")
  }

  it("validates responses") {
    TestHelper.expectSingleError("""
    {
      "name": "test",
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
          "operations": [
            {
              "method": "GET",
              "response": {
                "200": { "type": "[user]" }
              }
            }
          ]
        }
      }
    }
    """) should be("Resource[user] GET /users Unrecognized element[response]")
  }

  it("supports top level attributes") {
    val json = """
    {
      "name": "test",
      "attributes": [
        { "name": "foo", "value": { "bar": "baz" } }
      ]
    }
    """

    expectValid {
      TestHelper.serviceValidatorFromApiJson(json)
    }.attributes.map(_.name) should equal(Seq("foo"))
  }


}
