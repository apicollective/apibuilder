package core

import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class InlineUnionsSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {



    it("union types support inline models") {
    val json =
      """
    {
      "name": "API Builder",

      "unions": {
        "user": {
          "discriminator": "discriminator",
          "types": [
            {
              "type": {
                "model": "guest_user",
                "fields": [ { "name": "guid", "type": "uuid" }]
              }
            },
            {
              "type": {
                "model": "registered_user",
                "fields": [
                  { "name": "guid", "type": "uuid" },
                  { "name": "name", "type": "string" }
                ]
              }
            }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)
    service.models.find(_.name == "guest_user").getOrElse {
      sys.error("No guest_user model created")
    }.fields.map(_.name) should equal(
      Seq("guid")
    )

    service.models.find(_.name == "registered_user").getOrElse {
      sys.error("No registered_user model created")
    }.fields.map(_.name) should equal(
      Seq("guid", "name")
    )

    service.unions.find(_.name == "user").getOrElse {
      sys.error("No user union created")
    }.types.map(_.`type`) should equal(
      Seq("guest_user", "registered_user")
    )
  }

  it("supports an inline union in response") {
    val json = """
                 |{
                 |  "name": "API Builder",
                 |
                 |  "models": {
                 |    "user": {
                 |      "fields": [
                 |        { "name": "guid", "type": "uuid" }
                 |      ]
                 |    }
                 |  },
                 |
                 |  "resources": {
                 |    "user": {
                 |      "operations": [
                 |        {
                 |          "method": "POST",
                 |          "responses": {
                 |            "422": {
                 |              "type": {
                 |                "union": "[user_error]",
                 |                "types": [
                 |                  {
                 |                    "type": {
                 |                      "model": "simple_user_error",
                 |                      "fields": [
                 |                        {
                 |                          "name": "code",
                 |                          "type": {
                 |                            "enum": "simple_user_error_code",
                 |                            "values": [
                 |                              { "name": "name_cannot_be_empty" }
                 |                            ]
                 |                          }
                 |                        }
                 |                      ]
                 |                    }
                 |                  },
                 |
                 |                  {
                 |                    "type": {
                 |                      "model": "parameterized_user_error",
                 |                      "fields": [
                 |                        {
                 |                          "name": "code",
                 |                          "type": {
                 |                            "enum": "parameterized_user_error_code",
                 |                            "values": [
                 |                              { "name": "invalid_email_address" }
                 |                            ]
                 |                          }
                 |                        },
                 |                        {
                 |                          "name": "suggestion",
                 |                          "type": "string"
                 |                        }
                 |                      ]
                 |                    }
                 |                  }
                 |                ]
                 |              }
                 |            }
                 |          }
                 |        }
                 |      ]
                 |    }
                 |  }
                 |}
    """.stripMargin
    val service = setupValidApiJson(json)

    service.models.find(_.name == "simple_user_error").getOrElse {
      sys.error("No simple_user_error model created")
    }.fields.toList match {
      case code :: Nil => {
        code.name should equal("code")
        code.`type` should equal("simple_user_error_code")
      }
      case _ => sys.error("Expected one field")
    }

    service.enums.find(_.name == "simple_user_error_code").getOrElse {
      sys.error("No simple_user_error_code enum created")
    }.values.map(_.name) should equal(
      Seq("name_cannot_be_empty")
    )

    service.models.find(_.name == "parameterized_user_error").getOrElse {
      sys.error("No parameterized_user_error model created")
    }.fields.toList match {
      case code :: suggestion :: Nil => {
        code.name should equal("code")
        code.`type` should equal("parameterized_user_error_code")

        suggestion.name should equal("suggestion")
        suggestion.`type` should equal("string")
      }
      case _ => sys.error("Expected one field")
    }

    service.enums.find(_.name == "parameterized_user_error_code").getOrElse {
      sys.error("No parameterized_user_error_code enum created")
    }.values.map(_.name) should equal(
      Seq("invalid_email_address")
    )

    service.unions.find(_.name == "user_error").getOrElse {
      sys.error("no user_error union")
    }.types.map(_.`type`) should equal(
      Seq("simple_user_error", "parameterized_user_error")
    )

    val response = service.resources.head.operations.head.responses.head
    response.`type` should equal("[user_error]")
  }
}
