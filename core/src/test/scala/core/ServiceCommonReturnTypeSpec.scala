package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, AnyFunSpec}
import org.scalatest.Matchers

class ServiceCommonReturnTypeSpec extends AnyFunSpec with Matchers {

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
    TestHelper.serviceValidatorFromApiJson(json.format("user")).errors().mkString should be("")
    TestHelper.serviceValidatorFromApiJson(json.format("[user]")).errors().mkString should be("Resource[user] cannot have varying response types for 2xx response codes: [user], user")
  }

}
