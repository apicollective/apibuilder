package core

import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class InlineEnumInModelFieldsSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  def buildJson(
    required: Boolean
  ): String = {
    s"""
    {
      "name": "API Builder",

      "models": {
        "user": {
          "fields": [
            {
             "name": "type",
             "required": $required,
              "type": {
                "enum": "user_type",
                "values": [
                  { "name": "guest" },
                  { "name": "registered" }
                ]
              }
            }
          ]
        }
      }
    }
    """
  }

  it("supports an inline model field") {
    val service = setupValidApiJson(buildJson(required = true))
    val `enum` = service.enums.headOption.getOrElse {
      sys.error("No enum created")
    }
    `enum`.name should equal("user_type")
    `enum`.values.map(_.name) should equal(
      Seq("guest", "registered")
    )

    val field = service.models.headOption.getOrElse {
      sys.error("No model created")
    }.fields.headOption.getOrElse {
      sys.error("No field created")
    }

    field.name should equal("type")
    field.`type` should equal("user_type")
    field.required should be(true)
  }

  it("supports optional inline model field") {
    val service = setupValidApiJson(buildJson(required = false))
    service.models.headOption.getOrElse {
      sys.error("No model created")
    }.fields.headOption.getOrElse {
      sys.error("No field created")
    }.required should be(false)
  }
}
