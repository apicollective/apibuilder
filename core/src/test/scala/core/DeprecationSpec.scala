package core

import org.scalatest.{FunSpec, Matchers}

class DeprecationSpec extends FunSpec with Matchers {

  it("enum value") {
    val json = """
    {
      "name": "Api Doc",

      "enums": {
        "content_type": {
          "values": [
            { "name": "application/json", "deprecation": { "description": "blah" } },
            { "name": "application/xml" }
          ]
        }
      }
    }
    """

    val validator = ServiceValidator(TestHelper.serviceConfig, json)
    validator.errors.mkString("") should be("")
    val ct = validator.service.get.enums.find(_.name == "content_type").get

    ct.values.find(_.name == "application/json").get.deprecation.flatMap(_.description) should be(Some("blah"))
    ct.values.find(_.name == "application/xml").get.deprecation.flatMap(_.description) should be(None)
  }

}
