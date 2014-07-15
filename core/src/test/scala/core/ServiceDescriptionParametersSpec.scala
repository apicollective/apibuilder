package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ServiceDescriptionParametersSpec extends FunSpec with Matchers {

  val baseJson = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {

        "tag": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        },

        "user": {
          "fields": [
            { "name": "id", "type": "long" },
            { "name": "name", "type": "string" },
            { "name": "created_at", "type": "date-time-iso8601" },
            { "name": "tag", "type": "tag" }
          ]
        }
      },
      "resources": {
        "user": {
          "operations": [
            {
              "method": "GET",
              "path": "%s",
              "parameters": [
                { "name": "tags", "type": "map" }
              ]
            }
          ]
        }
      }
    }
  """

  it("numbers can be path parameters") {
    val json = baseJson.format("/:id")
    ServiceDescriptionValidator(json).errors.mkString("") should be("")
  }

  it("strings can be path parameters") {
    val json = baseJson.format("/:name")
    ServiceDescriptionValidator(json).errors.mkString("") should be("")
  }

  it("parameters not defined on the model are accepted (assumed strings)") {
    val json = baseJson.format("/:some_string")
    ServiceDescriptionValidator(json).errors.mkString("") should be("")
  }

  it("dates cannot be path parameters") {
    val json = baseJson.format("/:created_at")
    ServiceDescriptionValidator(json).errors.mkString("") should be("Resource[user] GET path parameter[created_at] has an invalid type[date-time-iso8601]. Only numbers and strings can be specified as path parameters")
  }

  it("other models cannot be path parameters") {
    val json = baseJson.format("/:tag")
    ServiceDescriptionValidator(json).errors.mkString("") should be("Resource[user] GET path parameter[tag] has an invalid type[tag]. Only numbers and strings can be specified as path parameters")
  }

  it("unsupported types declared as parameters are validated") {
    val json = baseJson.format("/:tags")
    ServiceDescriptionValidator(json).errors.mkString("") should be("Resource[user] GET path parameter[tags] has an invalid type[map]. Only numbers and strings can be specified as path parameters")
  }

}
