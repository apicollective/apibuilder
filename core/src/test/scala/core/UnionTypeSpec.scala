package core

import org.scalatest.{FunSpec, Matchers}

class UnionTypeSpec extends FunSpec with Matchers {

  it("accepts union types for singletons and lists") {
    val json = """
    {
      "name": "Union Types Test",

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
            { "name": "user", "type": "guest | registered_user" },
            { "name": "friends", "type": "[guest | registered_user]" }
          ]
        }

      }
    }
    """
    val validator = ServiceValidator(json)
    validator.errors.mkString("") should be("")
  }

}
