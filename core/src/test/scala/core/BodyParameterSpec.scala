package core

import lib.{Datatype, Methods, Primitives, Type, TypeKind}
import com.gilt.apidocspec.models._
import org.scalatest.{FunSpec, Matchers}

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
                { "name": "debug", "type": "%s" }
              ]
            }
          ]
        }
      }
    }
  """

  it("validates that body refers to a known model") {
    val validator = ServiceValidator(baseJson.format("POST", """{ "type": "foo" }""", "boolean"))
    validator.errors.mkString("") should be(s"Resource[message] POST /messages/:mimeType body: Type[foo] not found")
  }

  it("support primitive types in body") {
    val validator = ServiceValidator(baseJson.format("POST", """{ "type": "string", "description": "test" }""", "boolean"))
    validator.errors.mkString("") should be("")
    val model = validator.serviceDescription.get.models("message")
    val op = validator.serviceDescription.get.resources.values.head.operations.head
    op.body.map(_.`type`) should be(Some("string"))
    op.body.flatMap(_.description) should be(Some("test"))
  }

  it("support arrays of primitive types in body") {
    val validator = ServiceValidator(baseJson.format("POST", """{ "type": "[string]" }""", "boolean"))
    validator.errors.mkString("") should be("")
    val model = validator.serviceDescription.get.models("message")
    val op = validator.serviceDescription.get.resources.values.head.operations.head
    op.body.map(_.`type`) should be(Some("[string]"))
  }

  it("support enums in body") {
    val validator = ServiceValidator(baseJson.format("POST", """{ "type": "age_group" }""", "boolean"))
    validator.errors.mkString("") should be("")
    val model = validator.serviceDescription.get.models("message")
    val op = validator.serviceDescription.get.resources.values.head.operations.head
    op.body.map(_.`type`) should be(Some("age_group"))
  }

  it("support arrays of enums in body") {
    val validator = ServiceValidator(baseJson.format("POST", """{ "type": "[age_group]" }""", "boolean"))
    validator.errors.mkString("") should be("")
    val model = validator.serviceDescription.get.models("message")
    val op = validator.serviceDescription.get.resources.values.head.operations.head
    op.body.map(_.`type`) should be(Some("[age_group]"))
  }

  it("supports arrays of models in body") {
    val validator = ServiceValidator(baseJson.format("POST", """{ "type": "[message]" }""", "boolean"))
    validator.errors.mkString("") should be("")
    val model = validator.serviceDescription.get.models("message")
    val op = validator.serviceDescription.get.resources.values.head.operations.head
    op.body.map(_.`type`) should be(Some("[message]"))
  }

  it("validates if body is not a map") {
    val validator = ServiceValidator(baseJson.format("POST", """"string"""", "boolean"))
    validator.errors.mkString("") should be(s"""Resource[message] POST /messages/:mimeType: body declaration must be an object, e.g. { "type": "string" }""")
  }

  it("validates that body cannot be specified for GET, DELETE operations") {
    Methods.MethodsNotAcceptingBodies.foreach { method =>
      val validator = ServiceValidator(baseJson.format(method, """{ "type": "message" }""", "boolean"))
      validator.errors.mkString("") should be(s"Resource[message] $method /messages/:mimeType: Cannot specify body for HTTP method[$method]")
    }
  }

  it("supports models in body") {
    val validator = ServiceValidator(baseJson.format("POST", """{ "type": "message" }""", "boolean"))
    validator.errors.mkString("") should be("")
    val model = validator.serviceDescription.get.models("message")
    val op = validator.serviceDescription.get.resources.values.head.operations.head
    op.body.map(_.`type`) should be(Some("message"))
  }

  it("If body specified, all parameters are either PATH or QUERY") {
    val validator = ServiceValidator(baseJson.format("POST", """{ "type": "message" }""", "boolean"))
    validator.errors.mkString("") should be("")
    val op = validator.serviceDescription.get.resources.values.head.operations.head
    val params = op.parameters
    params.size should be(2)
    params.find(_.name == "mimeType").get.location should be(Some(ParameterLocation.Path))
    params.find(_.name == "debug").get.location should be(Some(ParameterLocation.Query))
  }

  it("body can be an array") {
    val validator = ServiceValidator(baseJson.format("POST", """{ "type": "[message]" }""", "boolean"))
    validator.errors.mkString("") should be("")
    val op = validator.serviceDescription.get.resources.values.head.operations.head
    op.body.map(_.`type`) should be(Some("[message]"))
  }

  it("validates missing datatype") {
    val baseJsonWithInvalidModel = baseJson.format("POST", """{ "type": "" }""", "age_group")
    val validator = ServiceValidator(baseJsonWithInvalidModel)
    validator.errors.mkString("") should be(s"Resource[message] POST /messages/:mimeType: Body missing type")
  }

  it("If body specified, parameters can be enums") {
    val baseJsonWithInvalidModel = baseJson.format("POST", """{ "type": "message" }""", "age_group")
    val validator = ServiceValidator(baseJsonWithInvalidModel)
    validator.errors.mkString("") should be("")
  }

  it("If body specified, parameters cannot be models") {
    val baseJsonWithInvalidModel = baseJson.format("POST", """{ "type": "message" }""", "message")
    val validator = ServiceValidator(baseJsonWithInvalidModel)
    validator.errors.mkString("") should be(s"Resource[message] POST /messages/:mimeType: Parameter[debug] has an invalid type[message]. Models are not supported as query parameters.")
  }

}
