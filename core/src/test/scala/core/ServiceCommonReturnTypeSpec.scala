package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ServiceCommonReturnTypeSpec extends FunSpec with Matchers {

  it("all 2xx return types must share a common types") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
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
    TestHelper.serviceValidatorFromApiJson(json.format("user")).errors.mkString should be("")
    TestHelper.serviceValidatorFromApiJson(json.format("[user]")).errors.mkString should be("Resource[user] cannot have varying response types for 2xx response codes: [user], user")
  }

}
