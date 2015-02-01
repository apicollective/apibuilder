package core

import org.scalatest.{FunSpec, Matchers}

class ServiceResourcesSpec extends FunSpec with Matchers {

  val baseJson = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",

      "enums": {
        "user_type": {
          "values": [
            { "name": "registered" },
            { "name": "guest" }
          ]
        }
      },

      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "long" },
            { "name": "type", "type": "user_type" }
          ]
        }
      },

      "resources": {
        "%s": {
          "operations": [
            {
              "method": "GET",
              "path": "/:id"
            }
          ]
        }
      }
    }
  """

  it("models can be resources") {
    val json = baseJson.format("user")
    val validator = ServiceValidator(TestHelper.serviceConfig, json)
    validator.errors.mkString("") should be("")
  }

  it("enums can be resources") {
    val json = baseJson.format("user_type")
    val validator = ServiceValidator(TestHelper.serviceConfig, json)
    validator.errors.mkString("") should be("")
  }

  it("lists cannot be resources") {
    val json = baseJson.format("[user]")
    val validator = ServiceValidator(TestHelper.serviceConfig, json)
    validator.errors.mkString("") should be("Resource[[user]] has an invalid type: must be a singleton (not a list nor map)")
  }

  it("maps cannot be resources") {
    val json = baseJson.format("[user]")
    val validator = ServiceValidator(TestHelper.serviceConfig, json)
    println(validator.errors.mkString("") )
    validator.errors.mkString("") should be("Resource[[user]] has an invalid type: must be a singleton (not a list nor map)")
  }

}
