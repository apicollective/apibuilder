package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class BrokenSpec extends FunSpec with Matchers {

  it("support arrays as types in fields") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "resources": {
        "vendors": {
          "fields": [
            { "name": "guid", "type": "string" },
            { "name": "tags", "type": "[string]" }
          ]
        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString should be("")
    val fields = validator.serviceDescription.get.resources.head.fields
    fields.find { _.name == "guid" }.get.multiple should be(false)
    fields.find { _.name == "tags" }.get.multiple should be(true)
  }


  it("support arrays as types in operations") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "resources": {
        "vendors": {
          "fields": [
            { "name": "guid", "type": "string" }
          ],
          "operations": [
            {
              "method": "POST",
              "parameters": [
                { "name": "guid", "type": "string" },
                { "name": "tag", "type": "[string]", "required": false }
              ],
              "responses": {
                "200": { "type": "vendors" }
              }
            }
          ]
        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString should be("")
    val operation = validator.serviceDescription.get.resources.head.operations.head
    operation.method should be("POST")
    operation.parameters.find { _.name == "guid" }.get.multiple should be(false)

    val guid = operation.parameters.find { _.name == "guid" }.get
    guid.multiple should be(false)
    guid.required should be(true)

    val tag = operation.parameters.find { _.name == "tag" }.get
    tag.multiple should be(true)
    tag.required should be(false)
  }


}
