package core

import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class InlineModelsSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  def buildJson(
    typ: String =
    """
      |{
      |  "type": {
      |    "model": "user_post_error",
      |    "description": "A test inline model",
      |    "fields": [
      |      { "name": "messages", "type": "[string]" }
      |    ]
      |  }
      |}
    """.stripMargin.trim
  ): String = {
    s"""
    {
      "name": "API Builder",

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

  it("supports an inline model field") {
    val service = setupValidApiJson(buildJson())
    val model = service.models.find(_.name == "user_post_error").getOrElse {
      sys.error("No user_post_error model created")
    }
    model.name should equal("user_post_error")
    model.description should equal(Some("A test inline model"))
    model.fields.toList match {
      case f :: Nil => {
        f.name should equal("messages")
        f.`type` should equal("[string]")
      }
      case _ => sys.error("Expected one field")
    }
  }

  it("supports an inline model containing a field with an inline enum") {
    val json = """
    {
      "name": "API Builder",

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
                "422": {
                  "type": {
                    "model": "[user_post_error]",
                    "fields": [
                      {
                        "name": "code",
                        "type": {
                          "enum": "user_post_error_code",
                          "values": [
                            { "name": "invalid_email_address" },
                            { "name": "name_cannot_be_empty" }
                          ]
                        }
                      }
                    ]
                  }
                }
              }
            }
          ]
        }
      }
    }
    """
    val service = setupValidApiJson(json)
    val model = service.models.find(_.name == "user_post_error").getOrElse {
      sys.error("No user_post_error model created")
    }
    model.name should equal("user_post_error")
    model.fields.toList match {
      case f :: Nil => {
        f.name should equal("code")
        f.`type` should equal("user_post_error_code")
      }
      case _ => sys.error("Expected one field")
    }

    val `enum` = service.enums.find(_.name == "user_post_error_code").getOrElse {
      sys.error("No user_post_error_code enum created")
    }
    `enum`.values.map(_.name) should equal(
      Seq("invalid_email_address", "name_cannot_be_empty")
    )

    val response = service.resources.head.operations.head.responses.head
    response.`type` should equal("[user_post_error]")
  }

}
