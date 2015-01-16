package core

import org.scalatest.{FunSpec, Matchers}

// Placeholder for when we do union types
class UnionTypeSpec extends FunSpec with Matchers {

  it("accepts union types for singletons and lists") {
    val json = """
    {
      "name": "Union Types Test",

      "unions": {
        "user": {
          "types": [
            { "type": "guest" },
            { "type": "registered_user" }
          ]
        }
      },

      "models": {

        "registered_user": {
          "fields": [
            { "name": "id", "type": "uuid" }
          ]
        },

        "guest": {
          "fields": [
            { "name": "id", "type": "uuid" }
          ]
        },

        "order": {
          "fields": [
            { "name": "id", "type": "uuid" },
            { "name": "user", "type": "user" }
          ]
        }

      }
    }
    """

    //val validator = ServiceValidator(TestHelper.serviceConfig, json)
    //validator.errors.mkString("") should be("")
  }

}
