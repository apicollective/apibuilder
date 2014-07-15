package core.generator

import core.{ Datatype, ServiceDescriptionValidator }

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
    val types = Datatype.QueryParameterTypes.map(_.name).sorted.mkString(" ")
    ServiceDescriptionValidator(json).errors.mkString("") should be(s"Resource[user] GET /users: Parameter[tag] has an invalid type[tag]. Must be one of: $types")
  }

  it("Play client supports all query data types") {
    // TODO
  }

}
