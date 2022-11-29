package core

import helpers.ApiJsonHelpers
import io.apibuilder.spec.v0.models.{ParameterLocation, Service}
import lib.Methods
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class BodyParameterSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  private[this] val baseJson = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "enums": {
        "age_group": {
          "values": [
            { "name": "Twenties" },
            { "name": "Thirties" }
          ]
        }
      },

      "models": {
        "message": {
          "fields": [
            { "name": "id", "type": "long" },
            { "name": "to", "type": "string" },
            { "name": "subject", "type": "string" }
          ]
        }
      },
      "resources": {
        "message": {
          "operations": [
            {
              "method": "%s",
              "path": "/:mimeType",
              "body": %s,
              "parameters": [
                { "name": "debug", "type": "%s" }
              ]
            }
          ]
        }
      }
    }
  """

  private[this] def setupValid(json: String): Service = {
    val service = setupValidApiJson(json)
    service.models.find(_.name == "message").getOrElse {
      sys.error("missing model message")
    }
    service
  }

  it("validates that body refers to a known model") {
    TestHelper.expectSingleError(baseJson.format("POST", """{ "type": "foo" }""", "boolean")) should be("Resource[message] POST /messages/:mimeType body type[foo] not found")
  }

  it("support primitive types in body") {
    val service = setupValid(baseJson.format("POST", """{ "type": "string", "description": "test" }""", "boolean"))
    val op = service.resources.head.operations.head
    op.body.map(_.`type`) should be(Some("string"))
    op.body.flatMap(_.description) should be(Some("test"))
  }

  it("support arrays of primitive types in body") {
    val op = setupValid(baseJson.format("POST", """{ "type": "[string]" }""", "boolean")).resources.head.operations.head
    op.body.map(_.`type`) should be(Some("[string]"))
  }

  it("support enums in body") {
    val op = setupValid(baseJson.format("POST", """{ "type": "age_group" }""", "boolean")).resources.head.operations.head
    op.body.map(_.`type`) should be(Some("age_group"))
  }

  it("support arrays of enums in body") {
    val service = setupValid(baseJson.format("POST", """{ "type": "[age_group]" }""", "boolean"))
    val op = service.resources.head.operations.head
    op.body.map(_.`type`) should be(Some("[age_group]"))
  }

  it("supports arrays of models in body") {
    val service = setupValid(baseJson.format("POST", """{ "type": "[message]" }""", "boolean"))
    val op = service.resources.head.operations.head
    op.body.map(_.`type`) should be(Some("[message]"))
  }

  it("validates if body is not a map") {
    TestHelper.expectSingleError(baseJson.format("POST", """"string"""", "boolean")) should be("Resource[message] POST /messages/:mimeType body, if present, must be an object")
  }

  it("validates that body cannot be specified for GET, DELETE operations") {
    Methods.MethodsNotAcceptingBodies.foreach { method =>
      TestHelper.expectSingleError(baseJson.format(method, """{ "type": "message" }""", "boolean")) should be(s"Resource[message] $method /messages/:mimeType Cannot specify body for HTTP method[$method]")
    }
  }

  it("validates that body can be specified for DELETE operations") {
    // we saw this in the magento swagger spec. HTTP says body CAN be provided and suggests
    // ignoring it
    val method = "POST"
    setupValid(baseJson.format(method, """{ "type": "message" }""", "boolean"))
  }

  it("supports models in body") {
    val service = setupValid(baseJson.format("POST", """{ "type": "message" }""", "boolean"))
    val op = service.resources.head.operations.head
    op.body.map(_.`type`) should be(Some("message"))
  }

  it("If body specified, all parameters are either PATH or QUERY") {
    val service = setupValid(baseJson.format("POST", """{ "type": "message" }""", "boolean"))
    val op = service.resources.head.operations.head
    val params = op.parameters
    params.size should be(2)
    params.find(_.name == "mimeType").get.location should be(ParameterLocation.Path)
    params.find(_.name == "debug").get.location should be(ParameterLocation.Query)
  }

  it("body can be an array") {
    val service = setupValid(baseJson.format("POST", """{ "type": "[message]" }""", "boolean"))
    val op = service.resources.head.operations.head
    op.body.map(_.`type`) should be(Some("[message]"))
  }

  it("validates missing datatype") {
    val baseJsonWithInvalidModel = baseJson.format("POST", """{ "type": "" }""", "age_group")
    TestHelper.expectSingleError(baseJsonWithInvalidModel) should be(s"Resource[message] POST /messages/:mimeType Body type must be a non empty string")
  }

  it("If body specified, parameters can be enums") {
    val baseJsonWithInvalidModel = baseJson.format("POST", """{ "type": "message" }""", "age_group")
    setupValid(baseJsonWithInvalidModel)
  }

  it("If body specified, parameters cannot be models") {
    val baseJsonWithInvalidModel = baseJson.format("POST", """{ "type": "message" }""", "message")
    TestHelper.expectSingleError(baseJsonWithInvalidModel) should be(s"Resource[message] POST /messages/:mimeType Parameter[debug] has an invalid type[message]. Interface, model and union types are not supported as query parameters.")
  }

}
