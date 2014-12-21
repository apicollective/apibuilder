package models

import core.ServiceValidator

import org.scalatest.{ FunSpec, Matchers }

class Play2ClientParametersSpec extends FunSpec with Matchers {

  describe("validates that models cannot be parameters") {
    val baseJson = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {

        "tag": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        },

        "user": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        }
      },
      "resources": {
        "user": {
          "operations": [
            {
              "method": "GET",
              "parameters": [
                { "name": "%s", "type": "%s" }
              ]
            }
          ]
        }
      }
    }
    """

    it("supports specifying a query parameter with model type") {
      val json = baseJson.format("tag", "tag")
      ServiceValidator(json).errors.mkString("") should be(s"Resource[user] GET /users: Parameter[tag] has an invalid type[tag]. Models are not supported as query parameters.")
    }
  }

}
