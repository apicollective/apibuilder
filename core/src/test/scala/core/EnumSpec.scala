package core

import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class EnumSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  private val baseJson: String = """
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
    TestHelper.expectSingleError(json) should be(
      "Enum[status] name[!] is invalid: Name can only contain a-z, A-Z, 0-9, - and _ characters, Name must start with a letter"
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
    setupValidApiJson(json)
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
    TestHelper.expectSingleError(json)should be(
      "Enum[status] value[open] appears more than once"
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
    setupValidApiJson(json).enums.headOption.getOrElse {
      sys.error("Missing enum")
    }.values.map(_.value) should equal(
      Seq(Some("is_open"), None)
    )
  }

}
