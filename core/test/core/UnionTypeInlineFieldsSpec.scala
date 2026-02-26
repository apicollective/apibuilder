package core

import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class UnionTypeInlineFieldsSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  it("union type with inline fields generates a model") {
    val json =
      """
    {
      "name": "API Builder",

      "unions": {
        "task_type": {
          "discriminator": "discriminator",
          "types": [
            {
              "type": "merge_person",
              "fields": [
                { "name": "user_id", "type": "string" },
                { "name": "person_id", "type": "string" }
              ]
            }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)

    val model = service.models.find(_.name == "merge_person").getOrElse {
      sys.error("No merge_person model created")
    }
    model.fields.map(_.name) should equal(Seq("user_id", "person_id"))
    model.fields.map(_.`type`) should equal(Seq("string", "string"))

    val union = service.unions.find(_.name == "task_type").getOrElse {
      sys.error("No task_type union created")
    }
    union.types.map(_.`type`) should equal(Seq("merge_person"))
  }

  it("union type with empty fields generates an empty model") {
    val json =
      """
    {
      "name": "API Builder",

      "unions": {
        "task_type": {
          "discriminator": "discriminator",
          "types": [
            { "type": "password_reset", "fields": [] }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)

    val model = service.models.find(_.name == "password_reset").getOrElse {
      sys.error("No password_reset model created")
    }
    model.fields should be(empty)

    val union = service.unions.find(_.name == "task_type").getOrElse {
      sys.error("No task_type union created")
    }
    union.types.map(_.`type`) should equal(Seq("password_reset"))
  }

  it("discriminator value defaults to the type name") {
    val json =
      """
    {
      "name": "API Builder",

      "unions": {
        "task_type": {
          "discriminator": "discriminator",
          "types": [
            { "type": "game", "fields": [] },
            {
              "type": "merge_person",
              "fields": [
                { "name": "user_id", "type": "string" },
                { "name": "person_id", "type": "string" }
              ]
            }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)
    val union = service.unions.find(_.name == "task_type").getOrElse {
      sys.error("No task_type union created")
    }

    val gameType = union.types.find(_.`type` == "game").getOrElse {
      sys.error("No game type found")
    }
    gameType.discriminatorValue should be(Some("game"))

    val mergeType = union.types.find(_.`type` == "merge_person").getOrElse {
      sys.error("No merge_person type found")
    }
    mergeType.discriminatorValue should be(Some("merge_person"))
  }

  it("explicit discriminator_value is preserved") {
    val json =
      """
    {
      "name": "API Builder",

      "unions": {
        "task_type": {
          "discriminator": "discriminator",
          "types": [
            { "type": "game", "discriminator_value": "custom_game", "fields": [] }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)
    val union = service.unions.find(_.name == "task_type").getOrElse {
      sys.error("No task_type union created")
    }
    union.types.head.discriminatorValue should be(Some("custom_game"))
  }

  it("mixed inline and pre-existing model references") {
    val json =
      """
    {
      "name": "API Builder",

      "models": {
        "existing_model": {
          "fields": [
            { "name": "id", "type": "uuid" }
          ]
        }
      },

      "unions": {
        "my_union": {
          "discriminator": "discriminator",
          "types": [
            { "type": "existing_model" },
            {
              "type": "inline_type",
              "fields": [
                { "name": "value", "type": "string" }
              ]
            }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)

    service.models.find(_.name == "existing_model").getOrElse {
      sys.error("No existing_model found")
    }.fields.map(_.name) should equal(Seq("id"))

    service.models.find(_.name == "inline_type").getOrElse {
      sys.error("No inline_type model created")
    }.fields.map(_.name) should equal(Seq("value"))

    val union = service.unions.find(_.name == "my_union").getOrElse {
      sys.error("No my_union union created")
    }
    union.types.map(_.`type`) should equal(Seq("existing_model", "inline_type"))
  }

  it("inline fields with various types") {
    val json =
      """
    {
      "name": "API Builder",

      "enums": {
        "operation": {
          "values": [
            { "name": "insert" },
            { "name": "update" },
            { "name": "delete" }
          ]
        }
      },

      "unions": {
        "task_type": {
          "discriminator": "discriminator",
          "types": [
            {
              "type": "notify",
              "description": "Notify directors of contact change",
              "fields": [
                { "name": "contact_id", "type": "string" },
                { "name": "operation", "type": "operation" },
                { "name": "changes", "type": "[string]", "required": false }
              ]
            }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)

    val model = service.models.find(_.name == "notify").getOrElse {
      sys.error("No notify model created")
    }
    model.fields.map(_.name) should equal(Seq("contact_id", "operation", "changes"))
    model.fields.find(_.name == "operation").get.`type` should equal("operation")
    model.fields.find(_.name == "changes").get.`type` should equal("[string]")
    model.fields.find(_.name == "changes").get.required should be(false)
    model.description should be(Some("Notify directors of contact change"))
  }

  it("backward compatible - unions without fields work as before") {
    val json =
      """
    {
      "name": "API Builder",

      "models": {
        "registered": {
          "fields": [
            { "name": "id", "type": "uuid" }
          ]
        },
        "guest": {
          "fields": [
            { "name": "id", "type": "uuid" }
          ]
        }
      },

      "unions": {
        "user": {
          "types": [
            { "type": "registered" },
            { "type": "guest" }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)
    val union = service.unions.find(_.name == "user").getOrElse {
      sys.error("No user union created")
    }
    union.types.map(_.`type`) should equal(Seq("registered", "guest"))

    service.models.map(_.name).sorted should equal(Seq("guest", "registered"))
  }

  it("duplicate name between inline fields and declared model produces error") {
    val json =
      """
    {
      "name": "API Builder",

      "models": {
        "merge_person": {
          "fields": [
            { "name": "id", "type": "uuid" }
          ]
        }
      },

      "unions": {
        "task_type": {
          "discriminator": "discriminator",
          "types": [
            {
              "type": "merge_person",
              "fields": [
                { "name": "user_id", "type": "string" }
              ]
            }
          ]
        }
      }
    }
    """

    TestHelper.expectSingleError(json) should be("Model[merge_person] appears more than once")
  }

  it("integration: multi-union spec with data and no-data types") {
    val json =
      """
    {
      "name": "API Builder",

      "enums": {
        "operation": {
          "values": [
            { "name": "insert" },
            { "name": "update" }
          ]
        }
      },

      "unions": {
        "task_type_global": {
          "discriminator": "discriminator",
          "types": [
            { "type": "password_reset", "fields": [] },
            { "type": "email_verification", "fields": [] },
            { "type": "optin_request", "fields": [
              { "name": "person_id", "type": "string" }
            ]}
          ]
        },
        "task_type_rallyd": {
          "discriminator": "discriminator",
          "types": [
            { "type": "game", "fields": [] },
            { "type": "merge_person", "fields": [
              { "name": "user_id", "type": "string" },
              { "name": "person_id", "type": "string" }
            ]},
            { "type": "notify", "fields": [
              { "name": "contact_id", "type": "string" },
              { "name": "op", "type": "operation" }
            ]}
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)

    service.unions.map(_.name).sorted should equal(Seq("task_type_global", "task_type_rallyd"))

    // no-data types generate empty models
    service.models.find(_.name == "password_reset").get.fields should be(empty)
    service.models.find(_.name == "game").get.fields should be(empty)

    // data types generate models with fields
    service.models.find(_.name == "merge_person").get.fields.map(_.name) should equal(Seq("user_id", "person_id"))
    service.models.find(_.name == "optin_request").get.fields.map(_.name) should equal(Seq("person_id"))
    service.models.find(_.name == "notify").get.fields.find(_.name == "op").get.`type` should equal("operation")

    // 6 types total = 6 models
    service.models should have size 6
  }
}
