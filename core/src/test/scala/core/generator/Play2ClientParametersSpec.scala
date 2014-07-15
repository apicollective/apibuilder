package core.generator

import core.ServiceDescriptionValidator

import org.scalatest.{ FunSpec, Matchers }

class Play2ClientParametersSpec extends FunSpec with Matchers {

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
            { "name": "id", "type": "long" }
          ]
        }
      },
      "resources": {
        "user": {
          "operations": [
            {
              "method": "GET",
              "parameters": [
                { "name": "%s", "type": "%s" }
              ]
            }
          ]
        }
      }
    }
  """

  it("supports specifying a query parameter with model type") {
    val json = baseJson.format("tag", "tag")
    ServiceDescriptionValidator(json).errors.mkString("") should be("")
    Play2ClientGenerator(json)
  }

  it("supports specifying a query parameter with model array type") {
    val json = baseJson.format("tags", "[tag]")
    ServiceDescriptionValidator(json).errors.mkString("") should be("")
    Play2ClientGenerator(json)
  }

}
