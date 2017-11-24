package core

import org.scalatest.{FunSpec, Matchers}

class EnumValueSpec extends FunSpec with Matchers {

  private[this] val baseJson: String = """
    {
      "name": "Api Doc",

      "enums": {
        %s
      }
    }
  """

  it("value must be a valid identifier") {
    val json = baseJson.format(
      """
        |"status": {
        |  "values": [
        |    { "name": "Open", "value": "!" }
        |  ]
        |}
      """.stripMargin
    )
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors() should be(
      Seq("Enum[status] value[!] is invalid: Name can only contain a-z, A-Z, 0-9 and _ characters and Name must start with a letter")
    )
  }

  it("values must be unique") {
    val json = baseJson.format(
      """
        |"status": {
        |  "values": [
        |    { "name": "Open" },
        |    { "name": "Closed", "value": "open" }
        |  ]
        |}
      """.stripMargin
    )
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors() should be(
      Seq("Enum[status] value[open] appears more than once")
    )
  }

  it("accepts value") {
    val json = baseJson.format(
      """
        |"status": {
        |  "values": [
        |    { "name": "Open", "value": "is_open" },
        |    { "name": "Closed" }
        |  ]
        |}
      """.stripMargin
    )
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.service().enums.headOption.getOrElse {
      sys.error("Missing enum")
    }.values.map(_.value) should equal(
      Seq(Some("is_open"), None)
    )
  }

}
