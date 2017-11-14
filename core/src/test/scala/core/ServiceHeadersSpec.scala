package core

import org.scalatest.{FunSpec, Matchers}

class ServiceHeadersSpec extends FunSpec with Matchers {

  describe("valid service") {
    val baseJson = """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },

      "headers": [
        { "name": "Content-Type", "type": "content_type" },
        { "name": "X-Foo", "type": "string", "description": "test", "default": "bar" },
        { "name": "X-Bar", "type": "string", "required": false },
        { "name": "X-Multi", "type": "[string]" }
      ],

      "enums": {
        "content_type": {
          "values": [
            { "name": "application/json" },
            { "name": "application/xml" }
          ]
        }
      }
    }
  """

    it("parses headers") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson)
      validator.errors().mkString("") should be("")
      val ctEnum = validator.service().enums.find(_.name == "content_type").get

      val ct = validator.service().headers.find(_.name == "Content-Type").get
      ct.name should be("Content-Type")
      ct.`type` should be("content_type")
      ct.default should be(None)
      ct.required should be(true)
      ct.description should be(None)
      ct.required should be(true)

      val foo = validator.service().headers.find(_.name == "X-Foo").get
      foo.name should be("X-Foo")
      foo.`type` should be("string")
      foo.default should be(Some("bar"))
      foo.description should be(Some("test"))
      foo.required should be(true)

      val bar = validator.service().headers.find(_.name == "X-Bar").get
      bar.name should be("X-Bar")
      bar.`type` should be("string")
      bar.default should be(None)
      bar.description should be(None)
      bar.required should be(false)

      val multi = validator.service().headers.find(_.name == "X-Multi").get
      multi.name should be("X-Multi")
      multi.`type` should be("[string]")
      multi.default should be(None)
      multi.description should be(None)
      multi.required should be(true)
    }
  }

  describe("validation of headers") {
    val baseJson = """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },

      "headers": [
        %s
      ]
    }
  """

    it("requires name") {
      val json = baseJson.format("""{ "type": "string" }""")
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors().mkString("") should be("Header[] Missing name")
    }

    it("requires type") {
      val json = baseJson.format("""{ "name": "no_type" }""")
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors().mkString("") should be("Header[no_type] Missing type")
    }

    it("validates type") {
      val json = baseJson.format("""{ "name": "invalid_type", "type": "integer" }""")
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors().mkString("") should be("Header[invalid_type] type[integer] is invalid: Must be a string or the name of an enum")
    }

    it("validates duplicates") {
      val json = baseJson.format("""{ "name": "dup", "type": "string" }, { "name": "dup", "type": "string" }""")
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors().mkString("") should be("Header[dup] appears more than once")
    }

  }

}
