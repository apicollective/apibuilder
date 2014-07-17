package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class BodyParameterSpec extends FunSpec with Matchers {

  val baseJson = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
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
              "body": { "type": "%s" },
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
    val validator = ServiceDescriptionValidator(baseJson.format("POST", "foo"))
    validator.errors.mkString("") should be(s"Resource[message] POST /messages/:mimeType body: Model named[foo] not found")
  }

  it("validates that body cannot be specified for GET, DELETE operations") {
    Util.MethodsNotAcceptingBodies.foreach { method =>
      val validator = ServiceDescriptionValidator(baseJson.format(method, "message"))
      validator.errors.mkString("") should be(s"Resource[message] $method /messages/:mimeType: Cannot specify body for HTTP method[$method]")
    }
  }

  it("operation body is parsed") {
    val validator = ServiceDescriptionValidator(baseJson.format("POST", "message"))
    validator.errors.mkString("") should be("")
    val model = validator.serviceDescription.get.models.find(_.name == "message").get
    val op = validator.serviceDescription.get.resources.head.operations.head
    op.body should be(Some(model))
  }

  it("If body specified, all parameters are either PATH or QUERY") {
    val validator = ServiceDescriptionValidator(baseJson.format("POST", "message"))
    validator.errors.mkString("") should be("")
    val op = validator.serviceDescription.get.resources.head.operations.head
    val params = op.parameters
    params.size should be(2)
    params.find(_.name == "mimeType").get.location should be(ParameterLocation.Path)
    params.find(_.name == "debug").get.location should be(ParameterLocation.Query)
  }

}
