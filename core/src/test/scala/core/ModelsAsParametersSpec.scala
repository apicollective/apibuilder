package core

import org.scalatest.{FunSpec, Matchers}

class ModelsAsParametersSpec extends FunSpec with Matchers {

  describe("validates that models cannot be parameters") {
    val baseJson = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {

        "tag": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        }
      },
      "resources": {
        "tag": {
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
println(ServiceDescriptionValidator(json).errors.mkString(""))
      ServiceDescriptionValidator(json).errors.mkString("") should be("Resource[tag] GET /tags: Parameter[tag] has an invalid type[tag]. Models are not supported as query parameters.")
    }
  }

}
