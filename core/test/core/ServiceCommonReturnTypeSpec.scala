package core

import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ServiceCommonReturnTypeSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  it("all 2xx return types must share a common types") {
    val json = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },
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
              "method": "GET",
              "responses": {
                "200": { "type": "[user]" }
              }
            },
            {
              "method": "POST",
              "responses": {
                "200": { "type": "user" },
                "201": { "type": "%s" }
              }
            }
          ]
        }
      }
    }
    """
    setupValidApiJson(json.format("user"))
    TestHelper.expectSingleError(json.format("[user]")) should be("Resource[user] cannot have varying response types for 2xx response codes: [user], user")
  }

}
