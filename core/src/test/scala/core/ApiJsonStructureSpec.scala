package core

import io.apibuilder.spec.v0.models.ParameterLocation
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ApiJsonStructureSpec extends AnyFunSpec with Matchers {

  it("name is required") {
    val json = """
    {
      "apidoc": { "version": "0.9.6" }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString should be("Missing name")
  }

  it("name must be a string") {
    val json = """
    {
      "name": { "foo": "bar" },
      "apidoc": { "version": "0.9.6" }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString should be("name must be a string")
  }

  it("base_url must be a string") {
    val json = """
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "base_url": { "foo": "bar" }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString should be("base_url, if present, must be a string")
  }

  it("description must be a string") {
    val json = """
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "description": ["test"]
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString should be("description, if present, must be a string")
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
      val validator = TestHelper.serviceValidatorFromApiJson(json.format(field))
      validator.errors().mkString("") should be(s"$field, if present, must be an array")
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
      val validator = TestHelper.serviceValidatorFromApiJson(json.format(field))
      validator.errors().mkString should be(s"$field, if present, must be an object")
    }
  }

  it("validates unknown keys") {
    val json = """
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "resource": []
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString should be("Unrecognized element[resource]")
  }

  it("validates multiple unknown keys") {
    val json = """
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "resource": [],
      "foo": []
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString should be("Unrecognized elements[foo, resource]")
  }

  it("validates models") {
    val json = """
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "description": []
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString(" ") should be("Model[user] description, if present, must be a string")
  }

  it("validates model fields are objects") {
    val json = """
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": ["test"]
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString(" ") should be("Model[user] elements of fields must be objects")
  }

  it("validates enums") {
    val json = """
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "enums": {
        "user": {
          "description": []
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString(" ") should be("Enum[user] description, if present, must be a string Enum[user] Missing values")
  }

  it("validates enum values are objects") {
    val json = """
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "enums": {
        "user": {
          "values": ["test"]
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString(" ") should be("Enum[user] elements of values must be objects")
  }

  it("validates unions") {
    val json = """
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "unions": {
        "user": {
          "description": []
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString(" ") should be("Union[user] description, if present, must be a string Union[user] Missing types")
  }

  it("validates union types are objects") {
    val json = """
    {
      "name": "test",
      "apidoc": { "version": "0.9.6" },
      "unions": {
        "user": {
          "types": ["test"]
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString(" ") should be("Union[user] elements of types must be objects")
  }

  it("validates responses") {
    val json = """
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
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString(" ") should be("Resource[user] GET /users Unrecognized element[response]")
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

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.service().attributes.map(_.name) should equal(Seq("foo"))
  }


}
