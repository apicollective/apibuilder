package core

import org.scalatest.{FunSpec, Matchers}

class ServiceDescriptionInternalReferencesSpec extends FunSpec with Matchers {

  it("Validates circular reference") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "svc-circular-reference",
      "models": {
        "foo": {
            "fields": [
                { "name": "bar", "type": "bar" }
            ]
        },
        "bar": {
            "fields": [
                { "name": "foo", "type": "foo" }
            ]
        }
      }
    }
    """

    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("Circular dependencies found while trying to resolve references for models: foo bar")
  }

  it("Is able to parse self reference") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "svc-reference",
      "models": {
        "userVariant": {
            "description": "variant set a user belongs to for a particular test.",
            "fields": [
                {
                    "name": "variantKey",
                    "type": "string"
                },
                {
                    "name": "parent",
                    "type": "userVariant",
                    "required": "false"
                }
            ]
        }
      }
    }
    """

    // TODO: Do we want to handle references back to self?
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("Circular dependencies found while trying to resolve references for models: userVariant")
  }

}
