package core

import com.gilt.apidoc.spec.v0.models.ParameterLocation
import org.scalatest.{FunSpec, Matchers}

class ApiJsonStructureSpec extends FunSpec with Matchers {

  it("name is required") {
    val json = """
    {}
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString should be("Missing name")
  }

  it("name must be a string") {
    val json = """
    { "name": { "foo": "bar" } }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString should be("name must be a string")
  }

  it("base_url must be a string") {
    val json = """
    {
      "name": "test",
      "base_url": { "foo": "bar" }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString should be("base_url, if present, must be a string")
  }

  it("description must be a string") {
    val json = """
    {
      "name": "test",
      "description": ["test"]
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString should be("description, if present, must be a string")
  }

  it("imports, headers must be arrays") {
    val json = """
    {
      "name": "test",
      "%s": { "foo": "bar" }
    }
    """

    Seq("imports", "headers").foreach { field =>
      val validator = TestHelper.serviceValidatorFromApiJson(json.format(field))
      validator.errors.mkString("") should be(s"$field, if present, must be an array")
    }
  }

  it("enums, models, unions, resources must be objects") {
    val json = """
    {
      "name": "test",
      "%s": []
    }
    """

    Seq("enums", "models", "unions", "resources").foreach { field =>
      val validator = TestHelper.serviceValidatorFromApiJson(json.format(field))
      validator.errors.mkString should be(s"$field, if present, must be an object")
    }
  }

  it("validates unknown keys") {
    val json = """
    {
      "name": "test",
      "resource": []
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString should be("Unrecognized element[resource]")
  }

  it("validates multiple unknown keys") {
    val json = """
    {
      "name": "test",
      "resource": [],
      "foo": []
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString should be("Unrecognized elements[foo, resource]")
  }

  it("validates models") {
    val json = """
    {
      "name": "test",
      "models": {
        "user": {
          "description": []
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString(" ") should be("Model[user] description, if present, must be a string Model[user] Missing fields")
  }

  it("validates model fields are objects") {
    val json = """
    {
      "name": "test",
      "models": {
        "user": {
          "fields": ["test"]
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString(" ") should be("Model[user] elements of fields must be objects")
  }

  it("validates enums") {
    val json = """
    {
      "name": "test",
      "enums": {
        "user": {
          "description": []
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString(" ") should be("Enum[user] description, if present, must be a string Enum[user] Missing values")
  }

  it("validates enum values are objects") {
    val json = """
    {
      "name": "test",
      "enums": {
        "user": {
          "values": ["test"]
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString(" ") should be("Enum[user] elements of values must be objects")
  }

  it("validates unions") {
    val json = """
    {
      "name": "test",
      "unions": {
        "user": {
          "description": []
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString(" ") should be("Union[user] description, if present, must be a string Union[user] Missing types")
  }

  it("validates union types are objects") {
    val json = """
    {
      "name": "test",
      "unions": {
        "user": {
          "types": ["test"]
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString(" ") should be("Union[user] elements of types must be objects")
  }

  it("validates responses") {
    val json = """
    {
      "name": "test",
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
    validator.errors.mkString(" ") should be("Resource[user] GET /users Unrecognized element[response]")
  }

}
