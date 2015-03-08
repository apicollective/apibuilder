package core

import org.scalatest.{FunSpec, Matchers}

class ServiceResourcesSpec extends FunSpec with Matchers {

  describe("with service") {
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

    it("models can be resources, with valid paths") {
      val json = baseJson.format("user")
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be("")

      val resource = validator.service.get.resources.head
      val op = resource.operations.head
      op.path should be ("/users/:id")
    }

    it("enums can be resources, with valid paths") {
      val json = baseJson.format("user_type")
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be("")

      val resource = validator.service.get.resources.head
      val op = resource.operations.head
      op.path should be ("/user_types/:id")
    }

    it("lists cannot be resources") {
      val json = baseJson.format("[user]")
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be("Resource[[user]] has an invalid type: must be a singleton (not a list nor map)")
    }

    it("maps cannot be resources") {
      val json = baseJson.format("[user]")
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be("Resource[[user]] has an invalid type: must be a singleton (not a list nor map)")
    }
=======
    val resource = validator.service.resources.head
    val op = resource.operations.head
    op.path should be ("/user_types/:id")
  }
>>>>>>> Remove infinite loop when validating service

  }

  it("allows empty path for resource") {
    val json = """
    {
      "name": "Api Doc",

      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        }
      },

      "resources": {
        "user": {
          "path": "",

          "operations": [
            {
              "method": "GET"
            },
            {
              "method": "GET",
              "path": "/foo"
            }
          ]
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString("") should be("")
    val operations = validator.service.get.resources.head.operations
    operations.head.path should be("/")
    operations.last.path should be("/foo")
  }

}
