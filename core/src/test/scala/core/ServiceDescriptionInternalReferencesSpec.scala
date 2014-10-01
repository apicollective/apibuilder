package core

import codegenerator.models.{Type, TypeKind}
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
    validator.errors.mkString("") should be("")
    validator.serviceDescription.get.models.find(_.name == "foo").get.fields.find(_.name == "bar").get.fieldtype match {
      case Type(TypeKind.Model, name, _) => name should be("bar")
      case other => fail("Expected field to be of type bar but was: " + other)
    }
    validator.serviceDescription.get.models.find(_.name == "bar").get.fields.find(_.name == "foo").get.fieldtype match {
      case Type(TypeKind.Model, name, _) => name should be("foo")
      case other => fail("Expected field to be of type foo but was: " + other)
    }
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

    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val model = validator.serviceDescription.get.models.find(_.name == "userVariant").get
    val parentField = model.fields.find(_.name == "parent").get
    parentField.fieldtype match {
      case Type(TypeKind.Model, name, _) => {
        name should be("userVariant")
      }
      case other => {
        fail("Expected field to be of type userVariant but was: " + other)
      }
    }
  }

}
