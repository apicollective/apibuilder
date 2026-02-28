package core

import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class UnionTypeFieldsSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  it("creates model from union type with fields") {
    val json = """
    {
      "name": "Union Type Fields Test",

      "unions": {
        "user": {
          "discriminator": "discriminator",
          "types": [
            {
              "type": "guest",
              "fields": [
                { "name": "id", "type": "uuid" },
                { "name": "nickname", "type": "string" }
              ]
            },
            {
              "type": "registered",
              "fields": [
                { "name": "id", "type": "uuid" },
                { "name": "email", "type": "string" }
              ]
            }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)
    val guestModel = service.models.find(_.name == "guest").getOrElse {
      sys.error("No guest model created")
    }
    guestModel.fields.map(_.name) should equal(Seq("id", "nickname"))

    val registeredModel = service.models.find(_.name == "registered").getOrElse {
      sys.error("No registered model created")
    }
    registeredModel.fields.map(_.name) should equal(Seq("id", "email"))

    service.unions.head.types.map(_.`type`) should equal(Seq("guest", "registered"))
  }

  it("creates empty model when fields is empty array") {
    val json = """
    {
      "name": "Union Type Fields Test",

      "unions": {
        "event": {
          "discriminator": "discriminator",
          "types": [
            {
              "type": "started",
              "fields": []
            },
            {
              "type": "completed",
              "fields": [
                { "name": "result", "type": "string" }
              ]
            }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)
    val startedModel = service.models.find(_.name == "started").getOrElse {
      sys.error("No started model created")
    }
    startedModel.fields should be(empty)

    val completedModel = service.models.find(_.name == "completed").getOrElse {
      sys.error("No completed model created")
    }
    completedModel.fields.map(_.name) should equal(Seq("result"))
  }

  it("rejects unknown type when fields is absent") {
    val json = """
    {
      "name": "Union Type Fields Test",

      "unions": {
        "user": {
          "types": [
            { "type": "unknown_type" }
          ]
        }
      }
    }
    """

    TestHelper.expectSingleError(json) should be(
      "Union[user] type[unknown_type] not found"
    )
  }

  it("rejects fields on primitive types") {
    val json = """
    {
      "name": "Union Type Fields Test",

      "unions": {
        "content": {
          "types": [
            {
              "type": "string",
              "fields": [
                { "name": "value", "type": "string" }
              ]
            }
          ]
        }
      }
    }
    """

    TestHelper.expectSingleError(json) should be(
      "Union[content] type[string] fields cannot be specified for primitive types"
    )
  }

  it("rejects fields on list types") {
    val json = """
    {
      "name": "Union Type Fields Test",

      "models": {
        "item": {
          "fields": [
            { "name": "id", "type": "uuid" }
          ]
        }
      },

      "unions": {
        "content": {
          "types": [
            {
              "type": "[item]",
              "fields": [
                { "name": "value", "type": "string" }
              ]
            }
          ]
        }
      }
    }
    """

    TestHelper.expectSingleError(json) should be(
      "Union[content] type[item] fields cannot be specified for list types"
    )
  }

  it("rejects fields on map types") {
    val json = """
    {
      "name": "Union Type Fields Test",

      "models": {
        "item": {
          "fields": [
            { "name": "id", "type": "uuid" }
          ]
        }
      },

      "unions": {
        "content": {
          "types": [
            {
              "type": "map[item]",
              "fields": [
                { "name": "value", "type": "string" }
              ]
            }
          ]
        }
      }
    }
    """

    TestHelper.expectSingleError(json) should be(
      "Union[content] type[item] fields cannot be specified for map types"
    )
  }

  it("allows fields alongside existing model") {
    val json = """
    {
      "name": "Union Type Fields Test",

      "models": {
        "registered": {
          "fields": [
            { "name": "id", "type": "uuid" }
          ]
        }
      },

      "unions": {
        "user": {
          "discriminator": "discriminator",
          "types": [
            { "type": "registered" },
            {
              "type": "guest",
              "fields": [
                { "name": "id", "type": "uuid" }
              ]
            }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)
    service.models.map(_.name).sorted should equal(Seq("guest", "registered"))
    service.unions.head.types.map(_.`type`) should equal(Seq("registered", "guest"))
  }

  it("passes fields through to service spec union type") {
    val json = """
    {
      "name": "Union Type Fields Test",

      "unions": {
        "user": {
          "discriminator": "discriminator",
          "types": [
            {
              "type": "guest",
              "fields": [
                { "name": "id", "type": "uuid" }
              ]
            }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)
    val unionType = service.unions.head.types.head
    unionType.`type` should equal("guest")
    val fields = unionType.fields.getOrElse(sys.error("Expected fields"))
    fields.map(_.name) should equal(Seq("id"))
    fields.head.`type` should equal("uuid")
  }

  it("union type fields is None when not specified") {
    val json = """
    {
      "name": "Union Type Fields Test",

      "models": {
        "guest": {
          "fields": [
            { "name": "id", "type": "uuid" }
          ]
        }
      },

      "unions": {
        "user": {
          "discriminator": "discriminator",
          "types": [
            { "type": "guest" }
          ]
        }
      }
    }
    """

    val service = setupValidApiJson(json)
    service.unions.head.types.head.fields should be(None)
  }

}
