package core

import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ServiceAnnotationsSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

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
    TestHelper.expectSingleError(json) shouldBe(
      "Model[user] Field[id] annotation[nope] is invalid. Annotations must be defined."
    )
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
    setupValidApiJson(json)
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
    TestHelper.expectSingleError(json) should be("Annotations must have a name")
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
    TestHelper.expectSingleError(json) should be("Annotation[@!#?@!] name is invalid: Name can only contain a-z, A-Z, 0-9, - and _ characters, Name must start with a letter")
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
    setupValidApiJson(json)
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
    TestHelper.expectSingleError(json) should be("Model[user] Field[id] Annotation[nodupes] appears more than once")
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
    val service = setupValidApiJson(json)
    service.annotations.size should be(2)
    service.models.head.fields.head.annotations.size should be(2)
  }
}
