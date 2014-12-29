package core

import com.gilt.apidocspec.models.Method
import org.scalatest.{FunSpec, Matchers}

class BrokenSpec extends FunSpec with Matchers {

  it("support arrays as types in fields") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
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
    val validator = ServiceValidator(json)
    validator.errors.mkString should be("")
    val fields = validator.service.get.models.head.fields
    fields.find { _.name == "guid" }.get.`type` should be("uuid")
    fields.find { _.name == "tags" }.get.`type` should be("[string]")
  }


  it("support arrays as types in operations") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "vendor": {
          "fields": [
            { "name": "guid", "type": "uuid" }
          ]
        }
      },
      "resources": [
        {
          "model": "vendor",
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
      ]
    }
    """
    val validator = ServiceValidator(json)
    validator.errors.mkString should be("")

    val operation = validator.service.get.resources.head.operations.head
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
