package core

import org.scalatest.{FunSpec, Matchers}

class ServiceMethodsSpec extends FunSpec with Matchers {

  it("missing method") {
    val json = """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },

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
            {}
          ]
        }
      }

    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString("") should be("Resource[user] /users Missing method")
  }

}
