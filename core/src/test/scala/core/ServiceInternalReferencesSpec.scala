package core

import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ServiceInternalReferencesSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  it("Validates circular reference") {
    val json = """
    {
      "name": "svc-circular-reference",
      "apidoc": { "version": "0.9.6" },
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

    val service = setupValidApiJson(json)

    val barField = service.models.find(_.name == "foo").get.fields.find(_.name == "bar").get
    barField.`type` should be("bar")

    val fooField = service.models.find(_.name == "bar").get.fields.find(_.name == "foo").get
    fooField.`type` should be("foo")
  }

  it("Is able to parse self reference") {
    val json = """
    {
      "name": "svc-reference",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user_variant": {
            "description": "variant set a user belongs to for a particular test.",
            "fields": [
                {
                    "name": "variant_key",
                    "type": "string"
                },
                {
                    "name": "parent",
                    "type": "user_variant",
                    "required": "false"
                }
            ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)
    val model = service.models.find(_.name == "user_variant").get
    val parentField = model.fields.find(_.name == "parent").get
    parentField.`type` should be("user_variant")
  }

}
