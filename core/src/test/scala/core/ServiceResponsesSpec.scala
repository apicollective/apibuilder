package core

import org.scalatest.{FunSpec, Matchers}

class ServiceResponsesSpec extends FunSpec with Matchers {

  val baseJson = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
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
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be(s"Resource[user] DELETE /users/:id Responses w/ code[$code] must return unit and not[user]")
    }
  }

  it("verifies that response defaults to type 204 Unit") {
    val json = baseJson.format("DELETE", "")
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString("") should be("")
    val response = validator.service.resources.head.operations.head.responses.find(_.code == 204).get
    response.`type` should be("unit")
  }

  it("does not allow explicit definition of 404, 5xx status codes") {
    Seq(404, 500, 501, 502).foreach { code =>
      val json = baseJson.format("DELETE", s""", "responses": { "$code": { "type": "unit" } } """)
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be(s"Resource[user] DELETE /users/:id has a response with code[$code] - this code cannot be explicitly specified")
    }
  }

  it("validates that responses is map from string to object") {
    val json = baseJson.format("DELETE", s""", "responses": { "204": "unit" } """)
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.contains(s"Resource[user] DELETE 204: Value must be a JsObject") should be(true)
  }

  it("generates a single error message for invalid 404 specification") {
    val json = baseJson.format("DELETE", s""", "responses": { "404": { "type": "user" } } """)
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString(" ") should be(s"Resource[user] DELETE /users/:id has a response with code[404] - this code cannot be explicitly specified")
  }

  it("generates an error message for an invalid method") {
    val json = baseJson.format("FOO", "")
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString(" ") should be("Resource[user] /users/:id Invalid HTTP method[FOO]. Must be one of: GET, POST, PUT, PATCH, DELETE, HEAD, CONNECT, OPTIONS, TRACE")
  }

  it("accepts lower and upper case method names") {
    val lower = baseJson.format("get", "")
    TestHelper.serviceValidatorFromApiJson(lower).errors.mkString(" ") should be("")

    val upper = baseJson.format("GET", "")
    TestHelper.serviceValidatorFromApiJson(upper).errors.mkString(" ") should be("")
  }

  it("generates an error message for a missing method") {
    val json = baseJson.format("", "")
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString(" ") should be(s"Resource[user] /users/:id Missing HTTP method")
  }

}
