package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ServiceDescriptionPathParametersSpec extends FunSpec with Matchers {

  val baseJson = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",

      "enums": {
        "age_group": {
          "values": [
            { "name": "Youth" },
            { "name": "Adult" }
          ]
        }
      },

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
            { "name": "tag", "type": "tag" },
            { "name": "tags", "type": "map" },
            { "name": "age_group", "type": "age_group" }
          ]
        }
      },
      "resources": {
        "user": {
          "operations": [
            {
              "method": "%s",
              "path": "%s"
            }
          ]
        }
      }
    }
  """

  it("numbers can be path parameters") {
    val json = baseJson.format("GET", "/:id")
    ServiceDescriptionValidator(json).errors.mkString("") should be("")
  }

  it("strings can be path parameters") {
    val json = baseJson.format("GET", "/:name")
    ServiceDescriptionValidator(json).errors.mkString("") should be("")
  }

  it("parameters not defined on the model are accepted (assumed strings)") {
    val json = baseJson.format("GET", "/:some_string")
    ServiceDescriptionValidator(json).errors.mkString("") should be("")
  }

  it("enums can be path parameters - assumed type is string") {
    val json = baseJson.format("GET", "/:age_group")
    ServiceDescriptionValidator(json).errors.mkString("") should be("")
  }

  it("dates cannot be path parameters") {
    val json = baseJson.format("GET", "/:created_at")
    ServiceDescriptionValidator(json).errors.mkString("") should be("Resource[user] GET path parameter[created_at] has an invalid type[date-time-iso8601]. Only numbers and strings can be specified as path parameters")
  }

  it("other models cannot be path parameters") {
    val json = baseJson.format("GET", "/:tag")
    ServiceDescriptionValidator(json).errors.mkString("") should be("Resource[user] GET path parameter[tag] has an invalid type[tag]. Only numbers and strings can be specified as path parameters")
  }

  it("unsupported types declared as parameters are validated") {
    val json = baseJson.format("POST", "/:tags")
    ServiceDescriptionValidator(json).errors.mkString("") should be("Resource[user] POST path parameter[tags] has an invalid type[map]. Only numbers and strings can be specified as path parameters")
  }

}
