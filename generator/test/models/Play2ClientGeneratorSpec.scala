package models

import generator.{ScalaDataType, ScalaService, ScalaClientMethodGenerator, ScalaClientMethodConfigs}
import core._
import org.scalatest.{ShouldMatchers, FunSpec}

class Play2ClientGeneratorSpec extends FunSpec with ShouldMatchers {

  val clientMethodConfig = new ScalaClientMethodConfigs.Play {
    override def responseClass = PlayFrameworkVersions.V2_2_x.config.responseClass
  }

  it("errorTypeClass") {
    val service = TestHelper.parseFile("../api/api.json").serviceDescription.get
    val ssd = new ScalaService(service)
    val resource = ssd.resources.find(_.model.name == "Organization").get
    val operation = resource.operations.find(_.method == "POST").get
    val errorResponse = operation.responses.find(_.code == 409).get
    errorResponse.errorClassName should be("ErrorsResponse")
    errorResponse.datatype should be(ScalaDataType.ScalaListType(ScalaDataType.ScalaModelType("apidoc.models", "error")))

    val contents = ScalaClientMethodGenerator(clientMethodConfig, ssd).errorPackage()
    TestHelper.assertEqualsFile("test/resources/generators/play2-client-generator-spec-errors-package.txt", contents)
  }

  it("only generates error wrappers for model classes (not primitives)") {
    val json = """
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
              "method": "GET",
              "path": "/:id",
              "responses": {
                "200": { "type": "user" },
                "409": { "type": "unit" }
              }
            }
          ]
        }
      }
    }

    """

    val validator = ServiceValidator(json)
    validator.errors.mkString("") should be("")
    val ssd = new ScalaService(validator.serviceDescription.get)
    ScalaClientMethodGenerator(clientMethodConfig, ssd).errorPackage() should be("")
  }

}
