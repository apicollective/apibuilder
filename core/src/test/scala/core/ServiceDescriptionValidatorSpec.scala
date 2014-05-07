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
    validator.errors.mkString should be("users.guid reference foo.bar is invalid. Resource[foo] does not exist")
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
    validator.errors.mkString should be("users.guid reference foo.bar is invalid. Resource[foo] does not have a field named[bar]")
    validator.isValid should be(false)
  }

  it("parses reference to field") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "resources": {
        "users": {
          "fields": [
            { "name": "guid", "type": "string", "format": "uuid" }
          ]
        },
        "accounts": {
          "fields": [
            { "name": "user_guid", "type": "string", "references": "users.guid" }
          ]
        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
  }

  it("defaults to a NoContent response") {
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
              "method": "DELETE",
              "path": "/:guid"
            }
          ]
        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val response = validator.serviceDescription.get.resources.head.operations.head.responses.head
    response.code should be(204)
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
                "200": { "type": "vendor" }
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

  it("includes path parameter in operations") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "resources": {
        "users": {
          "fields": [
            { "name": "guid", "type": "string", "format": "uuid" }
          ],
          "operations": [
            {
              "method": "DELETE",
              "path": "/:guid"
            }
          ]
        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val op = validator.serviceDescription.get.resources.head.operations.head
    op.parameters.map(_.name) should be(Seq("guid"))
  }

}
