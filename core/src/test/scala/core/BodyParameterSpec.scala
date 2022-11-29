package core

import lib.Methods
import io.apibuilder.spec.v0.models.ParameterLocation
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class BodyParameterSpec extends AnyFunSpec with Matchers {

  val baseJson = """
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

  it("validates that body refers to a known model") {
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("POST", """{ "type": "foo" }""", "boolean"))
    validator.errors().mkString("") should be("Resource[message] POST /messages/:mimeType body type[foo] not found")
  }

  it("support primitive types in body") {
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("POST", """{ "type": "string", "description": "test" }""", "boolean"))
    validator.errors().mkString("") should be("")
    validator.service().models.find(_.name == "message").get
    val op = validator.service().resources.head.operations.head
    op.body.map(_.`type`) should be(Some("string"))
    op.body.flatMap(_.description) should be(Some("test"))
  }

  it("support arrays of primitive types in body") {
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("POST", """{ "type": "[string]" }""", "boolean"))
    validator.errors().mkString("") should be("")
    validator.service().models.find(_.name == "message").get
    val op = validator.service().resources.head.operations.head
    op.body.map(_.`type`) should be(Some("[string]"))
  }

  it("support enums in body") {
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("POST", """{ "type": "age_group" }""", "boolean"))
    validator.errors().mkString("") should be("")
    validator.service().models.find(_.name == "message").get
    val op = validator.service().resources.head.operations.head
    op.body.map(_.`type`) should be(Some("age_group"))
  }

  it("support arrays of enums in body") {
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("POST", """{ "type": "[age_group]" }""", "boolean"))
    validator.errors().mkString("") should be("")
    validator.service().models.find(_.name == "message").get
    val op = validator.service().resources.head.operations.head
    op.body.map(_.`type`) should be(Some("[age_group]"))
  }

  it("supports arrays of models in body") {
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("POST", """{ "type": "[message]" }""", "boolean"))
    validator.errors().mkString("") should be("")
    validator.service().models.find(_.name == "message").get
    val op = validator.service().resources.head.operations.head
    op.body.map(_.`type`) should be(Some("[message]"))
  }

  it("validates if body is not a map") {
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("POST", """"string"""", "boolean"))
    validator.errors().mkString("") should be("Resource[message] POST /messages/:mimeType body, if present, must be an object")
  }

  it("validates that body cannot be specified for GET, DELETE operations") {
    Methods.MethodsNotAcceptingBodies.foreach { method =>
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format(method, """{ "type": "message" }""", "boolean"))
      validator.errors().mkString("") should be(s"Resource[message] $method /messages/:mimeType Cannot specify body for HTTP method[$method]")
    }
  }

  it("validates that body can be specified for DELETE operations") {
    // we saw this in the magento swagger spec. HTTP says body CAN be provided and suggests
    // ignoring it
    val method = "POST"
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format(method, """{ "type": "message" }""", "boolean"))
    validator.errors().isEmpty should be(true)
  }

  it("supports models in body") {
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("POST", """{ "type": "message" }""", "boolean"))
    validator.errors().mkString("") should be("")
    validator.service().models.find(_.name == "message").get
    val op = validator.service().resources.head.operations.head
    op.body.map(_.`type`) should be(Some("message"))
  }

  it("If body specified, all parameters are either PATH or QUERY") {
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("POST", """{ "type": "message" }""", "boolean"))
    validator.errors().mkString("") should be("")
    val op = validator.service().resources.head.operations.head
    val params = op.parameters
    params.size should be(2)
    params.find(_.name == "mimeType").get.location should be(ParameterLocation.Path)
    params.find(_.name == "debug").get.location should be(ParameterLocation.Query)
  }

  it("body can be an array") {
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("POST", """{ "type": "[message]" }""", "boolean"))
    validator.errors().mkString("") should be("")
    val op = validator.service().resources.head.operations.head
    op.body.map(_.`type`) should be(Some("[message]"))
  }

  it("validates missing datatype") {
    val baseJsonWithInvalidModel = baseJson.format("POST", """{ "type": "" }""", "age_group")
    val validator = TestHelper.serviceValidatorFromApiJson(baseJsonWithInvalidModel)
    validator.errors().mkString("") should be(s"Resource[message] POST /messages/:mimeType Body type must be a non empty string")
  }

  it("If body specified, parameters can be enums") {
    val baseJsonWithInvalidModel = baseJson.format("POST", """{ "type": "message" }""", "age_group")
    val validator = TestHelper.serviceValidatorFromApiJson(baseJsonWithInvalidModel)
    validator.errors().mkString("") should be("")
  }

  it("If body specified, parameters cannot be models") {
    val baseJsonWithInvalidModel = baseJson.format("POST", """{ "type": "message" }""", "message")
    val validator = TestHelper.serviceValidatorFromApiJson(baseJsonWithInvalidModel)
    validator.errors().mkString("") should be(s"Resource[message] POST /messages/:mimeType Parameter[debug] has an invalid type[message]. Interface, model and union types are not supported as query parameters.")
  }

}
