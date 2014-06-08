package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ServiceDescriptionDefaultsSpec extends FunSpec with Matchers {

  it("accepts strings as defaults for booleans") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "user": {
          "fields": [
            { "name": "guid", "type": "uuid" },
            { "name": "is_active", "type": "boolean", "default": "true" }
          ]
        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val isActiveField = validator.serviceDescription.get.models.head.fields.find { _.name == "is_active" }.get
    isActiveField.default should be(Some("true"))
  }

  it("validates duplicate models in the resources section") {
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
      "resources": [
        {
          "model": "user",
          "operations": [
            {
              "method": "DELETE"
            }
          ]
        },
        {
          "model": "user",
          "operations": [
            {
              "method": "GET"
            }
          ]
        }
      ]
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString should be("Model[user] cannot be mapped to more than one resource")
  }

}
