package core

import org.scalatest.{FunSpec, Matchers}

class ServiceMethodsSpec extends FunSpec with Matchers {

  it("missing method") {
    val json = """
    {
      "name": "Api Doc",
      "base_url": "http://localhost:9000",

      "models": {
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
              "name": "id",
              "type": "long"
            }
          ]
        }
      }

    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString("") should be("Resource[user] /users Missing HTTP method")
  }

}
