package core

import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json._

class ServiceResponsesSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  val baseJson = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        },
        "error": {
          "fields": [
            { "name": "code", "type": "string", "description": "Machine readable code for this specific error message" },
            { "name": "message", "type": "string", "description": "Description of the error" }
          ]
        }
      },
      "resources": {
        "user": {
          "operations": [
            {
              "method": "%s",
              "path": "/:id" %s
            }
          ]
        }
      }
    }
  """

  it("Returns error message if user specifies non Unit Response type") {
    Seq(204, 304).foreach { code =>
      val json = baseJson.format("DELETE", s""", "responses": { "$code": { "type": "user" } } """)
      TestHelper.expectSingleError(json) should be(s"Resource[user] DELETE /users/:id response code[$code] must return unit and not[user]")
    }
  }

  it("verifies that response defaults to type 204 Unit") {
    val json = baseJson.format("DELETE", "")
    val response = setupValidApiJson(json).resources.head.operations.head.responses.find(_.code == "204").get
    response.`type` should be("unit")
  }

  it("validates that responses is map from string to object") {
    val json = baseJson.format("DELETE", s""", "responses": { "204": "unit" } """)
    TestHelper.expectSingleError(json) should be("Resource[user] DELETE /users/:id 204: value must be an object")
  }

  it("generates an error message for an invalid method") {
    val json = baseJson.format("FOO", "")
    TestHelper.expectSingleError(json) should be("Resource[user] FOO /users/:id Invalid HTTP method[FOO]. Must be one of: GET, POST, PUT, PATCH, DELETE, HEAD, CONNECT, OPTIONS, TRACE")
  }

  it("accepts lower and upper case method names") {
    setupValidApiJson(baseJson.format("get", ""))
    setupValidApiJson(baseJson.format("GET", ""))
  }

  it("generates an error message for a missing method") {
    val json = baseJson.format("", "")
    TestHelper.expectSingleError(json) should be(s"Resource[user] /users/:id method must be a non empty string")
  }

  it("supports 'default' keyword for response codes") {
    val json = baseJson.format("DELETE", s""", "responses": { "default": { "type": "error" } } """)
    setupValidApiJson(json)
  }

  it("validates strings that are not 'default'") {
    val json = baseJson.format("DELETE", s""", "responses": { "def": { "type": "error" } } """)
    TestHelper.expectSingleError(json) should be("Response code must be an integer or the keyword 'default' and not[def]")
  }

  it("validates response codes are >= 100") {
    Seq(-50, 0, 50).foreach { code =>
      val json = baseJson.format("DELETE", s""", "responses": { "$code": { "type": "error" } } """)
      TestHelper.expectSingleError(json) should be(s"Response code[$code] must be >= 100")
    }
  }

  it("allows attributes in a response") {
      val json = baseJson.format("GET",
        s""",
           |"responses": {
           |  "422": {
           |    "type": "unit",
           |    "attributes": [
           |      {
           |        "name": "user_errors",
           |        "value": {
           |          "user_error_1" : {},
           |          "user_error_2" : {}
           |        }
           |      }
           |    ]
           |  }
           |}""".stripMargin)
      val response = setupValidApiJson(json).resources.head.operations.head.responses.find(_.code == "422").get
      response.`type` should be("unit")
      response.attributes.get.head.name should be("user_errors")
      response.attributes.get.head.value shouldBe a [JsObject]
  }

}
