package core

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class EnumSpec extends AnyFunSpec with Matchers {

  private[this] val baseJson: String = """
    {
      "name": "API Builder",

      "enums": {
        %s
      }
    }
  """

  it("name must be a valid identifier") {
    val json = baseJson.format(
      """
        |"status": {
        |  "values": [
        |    { "name": "!", "value": "foo" }
        |  ]
        |}
      """.stripMargin
    )
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors() should be(
      Seq("Enum[status] name[!] is invalid: Name can only contain a-z, A-Z, 0-9, - and _ characters, Name must start with a letter")
    )
  }

  it("value can be anything") {
    val json = baseJson.format(
      """
        |"status": {
        |  "values": [
        |    { "name": "foo", "value": "! $" }
        |  ]
        |}
      """.stripMargin
    )
    TestHelper.serviceValidatorFromApiJson(json).errors() should be(Nil)
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
