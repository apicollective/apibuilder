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

  it("should detect all required fields") {
    val validator = ServiceDescriptionValidator(" { } ")
    validator.isValid should be(false)
    validator.errors.mkString should be("Missing: base_url, name")
  }

  it("model that is missing fields") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "users": {

        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString should be("users model must have at least one field")
    validator.isValid should be(false)
  }

  it("reference that points to a non-existent model") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "users": {
          "fields": [
            { "name": "foo", "references": "foos.bar" }
          ]
        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString should be("users.foo has invalid reference to foo.bar. Model[foo] does not exist")
    validator.isValid should be(false)
  }

  it("reference that points to a non-existent field") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "users": {
          "fields": [
            { "name": "foo", "references": "users.bar" }
          ]
        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString should be("users.foo reference has invalid reference to users.bar. Model[users] does not have a field named[bar]")
    validator.isValid should be(false)
  }

  it("parses reference to field") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "users": {
          "fields": [
            { "name": "guid", "type": "uuid" }
          ]
        },
        "accounts": {
          "fields": [
            { "name": "user", "references": "users.guid" }
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
      "models": {
        "users": {
          "fields": [
            { "name": "guid", "type": "string" }
          ]
        }
      },
      "operations": {
        "users" => [
          {
            "method": "DELETE",
            "path": "/:guid"
          }
        ]
      ]
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val response = validator.serviceDescription.get.operations.head.responses.head
    response.code should be(204)
  }

  it("operations w/ a valid response validates correct") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "users": {
          "fields": [
            { "name": "guid", "type": "string" }
          ]
        }
      },
      "operations": {
        "users": [
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
      "models": {
        "user": {
          "fields": [
            { "name": "guid", "type": "uuid" }
          ]
        }
      },
      "operations": {
        "users": [
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
    val op = validator.serviceDescription.get.operations.head
    op.parameters.map(_.name) should be(Seq("guid"))
  }

}
