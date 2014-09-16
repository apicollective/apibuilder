package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class BodyParameterSpec extends FunSpec with Matchers {

  val baseJson = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",

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
                { "name": "debug", "type": "boolean" }
              ]
            }
          ]
        }
      }
    }
  """

  it("validates that body refers to a known model") {
    val validator = ServiceDescriptionValidator(baseJson.format("POST", """{ "type": "foo" }"""))
    validator.errors.mkString("") should be(s"Resource[message] POST /messages/:mimeType body: Model named[foo] not found")
  }

  it("support primitive types in body") {
    val validator = ServiceDescriptionValidator(baseJson.format("POST", """{ "type": "string" }"""))
    validator.errors.mkString("") should be("")
    val model = validator.serviceDescription.get.models.find(_.name == "message").get
    val op = validator.serviceDescription.get.resources.head.operations.head
    op.body should be(Some(PrimitiveBody(Datatype.StringType, false)))
  }

  it("support arrays of primitive types in body") {
    val validator = ServiceDescriptionValidator(baseJson.format("POST", """{ "type": "[string]" }"""))
    validator.errors.mkString("") should be("")
    val model = validator.serviceDescription.get.models.find(_.name == "message").get
    val op = validator.serviceDescription.get.resources.head.operations.head
    op.body should be(Some(PrimitiveBody(Datatype.StringType, true)))
  }

  it("support enums in body") {
    val validator = ServiceDescriptionValidator(baseJson.format("POST", """{ "type": "age_group" }"""))
    validator.errors.mkString("") should be("")
    val model = validator.serviceDescription.get.models.find(_.name == "message").get
    val op = validator.serviceDescription.get.resources.head.operations.head
    op.body should be(Some(EnumBody("age_group", false)))
  }

  it("support arrays of enums in body") {
    val validator = ServiceDescriptionValidator(baseJson.format("POST", """{ "type": "[age_group]" }"""))
    validator.errors.mkString("") should be("")
    val model = validator.serviceDescription.get.models.find(_.name == "message").get
    val op = validator.serviceDescription.get.resources.head.operations.head
    op.body should be(Some(EnumBody("age_group", true)))
  }

  it("supports arrays of models in body") {
    val validator = ServiceDescriptionValidator(baseJson.format("POST", """{ "type": "[message]" }"""))
    validator.errors.mkString("") should be("")
    val model = validator.serviceDescription.get.models.find(_.name == "message").get
    val op = validator.serviceDescription.get.resources.head.operations.head
    op.body should be(Some(ModelBody(model.name, true)))
  }

  it("validates if body is not a map") {
    val validator = ServiceDescriptionValidator(baseJson.format("POST", """"string""""))
    validator.errors.mkString("") should be(s"""Resource[message] POST /messages/:mimeType: body declaration must be an object, e.g. { "type": "string" }""")
  }

  it("validates that body cannot be specified for GET, DELETE operations") {
    Util.MethodsNotAcceptingBodies.foreach { method =>
      val validator = ServiceDescriptionValidator(baseJson.format(method, """{ "type": "message" }"""))
      validator.errors.mkString("") should be(s"Resource[message] $method /messages/:mimeType: Cannot specify body for HTTP method[$method]")
    }
  }

  it("supports models in body") {
    val validator = ServiceDescriptionValidator(baseJson.format("POST", """{ "type": "message" }"""))
    validator.errors.mkString("") should be("")
    val model = validator.serviceDescription.get.models.find(_.name == "message").get
    val op = validator.serviceDescription.get.resources.head.operations.head
    op.body should be(Some(ModelBody(model.name, false)))
  }

  it("If body specified, all parameters are either PATH or QUERY") {
    val validator = ServiceDescriptionValidator(baseJson.format("POST", """{ "type": "message" }"""))
    validator.errors.mkString("") should be("")
    val op = validator.serviceDescription.get.resources.head.operations.head
    val params = op.parameters
    params.size should be(2)
    params.find(_.name == "mimeType").get.location should be(ParameterLocation.Path)
    params.find(_.name == "debug").get.location should be(ParameterLocation.Query)
  }

  it("body can be an array") {
    val validator = ServiceDescriptionValidator(baseJson.format("POST", """{ "type": "[message]" }"""))
    validator.errors.mkString("") should be("")
    val op = validator.serviceDescription.get.resources.head.operations.head
    op.body should be(Some(ModelBody("message", true)))
  }

}
