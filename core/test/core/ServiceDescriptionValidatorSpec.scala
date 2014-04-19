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
    validator.errors.mkString should be("Invalid JSON")
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
    validator.errors.mkString should be("Reference foo.bar points to a non existent resource")
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
    validator.errors.mkString should be("Reference users.bar points to a non existent field")
    validator.isValid should be(false)
  }

  it("multiple errors w/ same text are collapsed into one message") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "resources": {
        "users": {
          "fields": [
            { "name": "a", "type": "string", "references": "users.bar" },
            { "name": "b", "type": "string", "references": "users.bar" },
            { "name": "c", "type": "string", "references": "users.baz" }
          ]
        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString(", ") should be("Reference users.bar points to a non existent field, Reference users.baz points to a non existent field")
    validator.isValid should be(false)
  }

}
