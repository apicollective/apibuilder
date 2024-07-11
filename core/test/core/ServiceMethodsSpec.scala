package core

import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ServiceMethodsSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  it("missing method") {
    val json = """
    {
      "name": "API Builder",
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

    TestHelper.expectSingleError(json) should be("Resource[user] /users Missing method")
  }

}
