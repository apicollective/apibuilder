package core

import org.scalatest.{FunSpec, Matchers}

class ServiceDefaultsSpec extends FunSpec with Matchers {

  it("accepts defaults for date-iso8601") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "user": {
          "fields": [
            { "name": "created_at", "type": "date-iso8601", "default": "2014-01-01" }
          ]
        }
      }
    }
    """
    val validator = ServiceValidator(TestHelper.serviceConfig, json)
    validator.errors.mkString("") should be("")

    val createdAt = validator.service.get.models.head.fields.find { _.name == "created_at" }.get
    createdAt.default should be(Some("2014-01-01"))
  }

  it("accepts strings and values as defaults for booleans") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "user": {
          "fields": [
            { "name": "is_active", "type": "boolean", "default": "true", "required": "true" },
            { "name": "is_athlete", "type": "boolean", "default": "false", "required": "false" }
          ]
        }
      }
    }
    """
    val validator = ServiceValidator(TestHelper.serviceConfig, json)
    validator.errors.mkString("") should be("")

    val isActiveField = validator.service.get.models.head.fields.find { _.name == "is_active" }.get
    isActiveField.default should be(Some("true"))
    isActiveField.required should be(true)

    val isAthleteField = validator.service.get.models.head.fields.find { _.name == "is_athlete" }.get
    isAthleteField.default should be(Some("false"))
    isAthleteField.required should be(false)
  }

  it("respects option type for required fields") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "user": {
          "fields": [
            { "name": "is_active", "type": "boolean" },
            { "name": "is_athlete", "type": "option[boolean]" },
            { "name": "friends", "type": "option[[string]]" }
          ]
        }
      }
    }
    """
    val validator = ServiceValidator(TestHelper.serviceConfig, json)
    validator.errors.mkString("") should be("")

    val isActiveField = validator.service.get.models.head.fields.find { _.name == "is_active" }.get
    isActiveField.required should be(true)

    val isAthleteField = validator.service.get.models.head.fields.find { _.name == "is_athlete" }.get
    isAthleteField.required should be(false)

    val friendsField = validator.service.get.models.head.fields.find { _.name == "friends" }.get
    friendsField.required should be(false)
    friendsField.`type` should be("[string]")
  }

  it("rejects invalid boolean defaults") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "user": {
          "fields": [
            { "name": "is_active", "type": "boolean", "default": 1 }
          ]
        }
      }
    }
    """
    val validator = ServiceValidator(TestHelper.serviceConfig, json)
    validator.errors.mkString("") should be("user.is_active Value[1] is not a valid boolean. Must be one of: true, false")
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
      "resources": {
        "user": {
          "operations": [
            {
              "method": "DELETE"
            }
          ]
        },
        "user": {
          "operations": [
            {
              "method": "GET"
            }
          ]
        }
      }
    }
    """
    val validator = ServiceValidator(TestHelper.serviceConfig, json)
    validator.errors.mkString should be("Resource[user] cannot appear multiple times")
  }

}
