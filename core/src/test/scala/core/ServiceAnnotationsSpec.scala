package core

import io.apibuilder.spec.v0.models.ParameterLocation
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ServiceAnnotationsSpec extends AnyFunSpec with Matchers {

  it("fields specifying an undefined annotation are NOT allowed") {
    val json =
      """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "long", "annotations": ["nope"]}
          ]
        }
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString should not be("")
  }

  it("a defined annotation is allowed") {
    val json =
      """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "annotations": {
        "okay": {}
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString should be("")
  }

  it("a defined annotation must have a name") {
    val json =
      """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "annotations": {
        "": {}
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString should be("Annotations must have a name")
  }

  it("a defined annotation must have a name without special characters") {
    val json =
      """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "annotations": {
        "@!#?@!": {}
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString should be("Annotation[@!#?@!] name is invalid: Name can only contain a-z, A-Z, 0-9, - and _ characters, Name must start with a letter")
  }

  it("fields specifying a defined annotation are allowed") {
    val json =
      """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "annotations": {
        "okay": {}
      },
      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "long", "annotations": ["okay"]}
          ]
        }
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString should be("")
  }

  it("fields specifying duplicated annotations are NOT allowed") {
    val json =
      """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "annotations": {
        "nodupes": {}
      },
      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "long", "annotations": ["nodupes", "nodupes"]}
          ]
        }
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().nonEmpty should be(true)
    validator.errors().mkString should be("Model[user] Field[id] Annotation[nodupes] appears more than once")
  }

  it("fields with multiple annotations allowed") {
    val json =
      """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "annotations": {
        "one": {},
        "two": {}
      },
      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "long", "annotations": ["one", "two"]}
          ]
        }
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString should be("")
    validator.service().annotations.size should be(2)
    validator.service().models.head.fields.head.annotations.size should be(2)
  }
}
