package core

import helpers.ApiJsonHelpers
import io.apibuilder.spec.v0.models.Method
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class BrokenSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  it("support arrays as types in fields") {
    val json = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "vendor": {
          "fields": [
            { "name": "guid", "type": "uuid" },
            { "name": "tags", "type": "[string]" }
          ]
        }
      }
    }
    """

    val fields = setupValidApiJson(json).models.head.fields
    fields.find { _.name == "guid" }.get.`type` should be("uuid")
    fields.find { _.name == "tags" }.get.`type` should be("[string]")
  }


  it("support arrays as types in operations") {
    val json = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "vendor": {
          "fields": [
            { "name": "guid", "type": "uuid" }
          ]
        }
      },
      "resources": {
        "vendor": {
          "operations": [
            {
              "method": "POST",
              "parameters": [
                { "name": "guid", "type": "uuid" },
                { "name": "tag", "type": "[string]", "required": false }
              ],
              "responses": {
                "200": { "type": "vendor" }
              }
            }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)

    val operation = service.resources.head.operations.head
    operation.method should be(Method.Post)
    operation.parameters.find { _.name == "guid" }.get.`type` should be("uuid")

    val guid = operation.parameters.find { _.name == "guid" }.get
    guid.`type` should be("uuid")
    guid.required should be(true)

    val tag = operation.parameters.find { _.name == "tag" }.get
    tag.`type` should be("[string]")
    tag.required should be(false)
  }


}
