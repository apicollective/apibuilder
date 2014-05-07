package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ServiceDescriptionValidatorSpec extends FunSpec with Matchers {

  it("should detect empty inputs") {
    val validator = ServiceDescriptionValidator("")
    validator.isValid should be(false)
    validator.errors.mkString should be("No Data")
  }

  it("should detect invalid json") {
    val validator = ServiceDescriptionValidator(" { ")
    validator.isValid should be(false)
    validator.errors.mkString.indexOf("expected close marker") should be >= 0
  }

  describe("missing fields") {

    it("should detect all required fields") {
      val validator = ServiceDescriptionValidator(" { } ")
      validator.isValid should be(false)
      validator.errors.mkString should be("Missing: base_url, name, resources")
    }

    it("should detect only missing resources") {
      val json = """
      {
        "base_url": "http://localhost:9000",
        "name": "Api Doc"
      }
      """
      val validator = ServiceDescriptionValidator(json)
      validator.isValid should be(false)
      validator.errors.mkString should be("Missing: resources")
    }

  }

  it("empty resources") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "resources": {}
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString should be("Must have at least one resource")
    validator.isValid should be(false)
  }

  it("single resource that is missing fields") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "resources": {
        "users": {

        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString should be("users resource must have at least one field")
    validator.isValid should be(false)
  }

  it("reference that points to a non-existent resource") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "resources": {
        "users": {
          "fields": [
            { "name": "guid", "type": "string", "references": "foo.bar" }
          ]
        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString should be("Resource users field guid reference foo.bar points to a non existent resource (foo)")
    validator.isValid should be(false)
  }

  it("reference that points to a non-existent field") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "resources": {
        "users": {
          "fields": [
            { "name": "guid", "type": "string", "references": "users.bar" }
          ]
        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString should be("Resource users field guid reference users.bar points to a non existent field (bar)")
    validator.isValid should be(false)
  }

  it("operations must have at least one response") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "resources": {
        "users": {
          "fields": [
            { "name": "guid", "type": "string" }
          ],
          "operations": [
            {
              "method": "GET",
              "path": "/:guid",
              "parameters": [
                { "name": "guid", "type": "string" }
              ]
            }
          ]
        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString(", ") should be("users GET /:guid missing responses element")
    validator.isValid should be(false)
  }

  it("operations w/ a valid response validates correct") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "resources": {
        "users": {
          "fields": [
            { "name": "guid", "type": "string" }
          ],
          "operations": [
            {
              "method": "GET",
              "path": "/:guid",
              "parameters": [
                { "name": "guid", "type": "string" }
              ],
              "responses": {
                "200" => { "type": "vendor" }
              }
            }
          ]
        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString(", ") should be("")
    validator.isValid should be(true)
  }

  it("support arrays as types in fields") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "resources": {
        "vendors": {
          "fields": [
            { "name": "guid", "type": "string" },
            { "name": "tags", "type": "string", "multiple": true }
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
                "200" => { "type": "vendors" }
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
