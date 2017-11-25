package core

import org.scalatest.{FunSpec, Matchers}

class InlineEnumsSpec extends FunSpec with Matchers {

  def buildJson(
    typ: String =
      """
        |{
        |  "type": {
        |    "enum": "user_post_error_code",
        |    "description": "A test enum",
        |    "values": [
        |      { "name": "invalid_email" }
        |    ]
        |  }
        |}
      """.stripMargin.trim,
    extras: String = ""    
  ): String = {
    s"""
    {
      "name": "Api Doc",
$extras
      "models": {
        "user": {
          "fields": [
            { "name": "guid", "type": "uuid" }
          ]
        }
      },

      "resources": {
        "user": {
          "operations": [
            {
              "method": "POST",
              "responses": {
                "422": $typ
              }
            }
          ]
        }
      }
    }
    """
  }

  it("returns a good error message on invalid type") {
    TestHelper.serviceValidatorFromApiJson(buildJson("[]")).errors() should equal(
      Seq("Resource[user] POST /users 422: value must be an object")
    )

    TestHelper.serviceValidatorFromApiJson(buildJson(
      """
        |{ "type": {} }
      """.stripMargin
    )).errors() should equal(
      Seq("Resource[user] POST /users 422 type: must specify field 'enum', 'model' or 'union'")
    )

    TestHelper.serviceValidatorFromApiJson(buildJson(
      """
        |{ "type": [] }
      """.stripMargin
    )).errors() should equal(
      Seq("Resource[user] POST /users 422 type: must be a string or an object")
    )
  }

  it("validates inline enum names") {
    val json = buildJson(
      extras =
        """
          |"enums": {
          |  "user_post_error_code": {
          |    "values": [
          |      { "name": "invalid_email" }
          |    ]
          |  }
          |},
        """.stripMargin.trim
    )

    TestHelper.serviceValidatorFromApiJson(json).errors() should equal(
      Seq("Enum[user_post_error_code] appears more than once")
    )
  }

  it("supports an inline enum") {
    val service = TestHelper.serviceValidatorFromApiJson(buildJson()).service()
    val enum = service.enums.headOption.getOrElse {
      sys.error("No enum created")
    }
    enum.name should equal("user_post_error_code")
    enum.description should equal(Some("A test enum"))
    enum.values.map(_.name) should equal(Seq("invalid_email"))

    val response = service.resources.head.operations.head.responses.head
    response.`type` should equal("user_post_error_code")
  }

  it("supports an inline enum defined as a list") {
    val service = TestHelper.serviceValidatorFromApiJson(buildJson(
      """
        |{
        |  "type": {
        |    "enum": "[user_post_error_code]",
        |    "values": [
        |      { "name": "invalid_email" }
        |    ]
        |  }
        |}
      """.stripMargin.trim
    )).service()
    val enum = service.enums.headOption.getOrElse {
      sys.error("No enum created")
    }
    enum.name should equal("user_post_error_code")
    enum.values.map(_.name) should equal(Seq("invalid_email"))

    val response = service.resources.head.operations.head.responses.head
    response.`type` should equal("[user_post_error_code]")
  }

  it("supports an inline enum defined as a map") {
    val service = TestHelper.serviceValidatorFromApiJson(buildJson(
      """
        |{
        |  "type": {
        |    "enum": "map[user_post_error_code]",
        |    "values": [
        |      { "name": "invalid_email" }
        |    ]
        |  }
        |}
      """.stripMargin.trim
    )).service()
    val enum = service.enums.headOption.getOrElse {
      sys.error("No enum created")
    }
    enum.name should equal("user_post_error_code")
    enum.values.map(_.name) should equal(Seq("invalid_email"))

    val response = service.resources.head.operations.head.responses.head
    response.`type` should equal("map[user_post_error_code]")
  }

}
