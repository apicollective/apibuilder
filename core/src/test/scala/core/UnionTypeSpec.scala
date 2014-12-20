package core

import org.scalatest.{FunSpec, Matchers}

class UnionTypeSpec extends FunSpec with Matchers {

  it("accepts defaults for date-iso8601") {
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
            { "name": "user", "type": "union[guest, registered_user]" }
          ]
        }

      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
  }

}
