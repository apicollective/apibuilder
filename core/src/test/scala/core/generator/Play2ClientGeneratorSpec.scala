package core.generator

import core.{ ServiceDescriptionValidator, TestHelper }
import org.scalatest.{ ShouldMatchers, FunSpec }

class Play2ClientGeneratorSpec extends FunSpec with ShouldMatchers {

  it("errorTypeClass") {
    val service = TestHelper.parseFile("api/api.json").serviceDescription.get
    val ssd = new ScalaServiceDescription(service)
    val resource = ssd.resources.find(_.model.name == "Organization").get
    val operation = resource.operations.find(_.method == "POST").get
    val errorResponse = operation.responses.find(_.code == 409).get

    val target = """
case class ErrorsResponse(response: play.api.libs.ws.WSResponse) extends Exception {

  lazy val errors = response.json.as[scala.collection.Seq[apidoc.models.Error]]

}
""".trim
    Play2ClientGenerator.errorTypeClass(errorResponse).trim should be(target)
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

    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val ssd = new ScalaServiceDescription(validator.serviceDescription.get)
    Play2ClientGenerator.errors(ssd) should be(None)
  }

}
