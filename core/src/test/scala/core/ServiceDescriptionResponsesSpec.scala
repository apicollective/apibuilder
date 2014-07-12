package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ServiceDescriptionResponsesSpec extends FunSpec with Matchers {

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
              "method": "DELETE",
              "path": "/:id" %s
            }
          ]
        }
      }
    }
  """

  it("Returns error message if user specifies non Unit Response type") {
    val json = baseJson.format(""", "responses": { "204": { "type": "user" } } """)
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("Resource[user] DELETE /users/:id has a response code of 204 (No Content). Cannot specify a type[user] for this reponse code.")
  }

  it("verifies that response defaults to type 204 Unit") {
    val json = baseJson.format("")
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val response = validator.serviceDescription.get.resources.head.operations.head.responses.head
    response.code should be(204)
    response.datatype should be("unit")
  }

}
