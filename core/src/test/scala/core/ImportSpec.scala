package core

import org.scalatest.{FunSpec, Matchers}

class ImportTypeSpec extends FunSpec with Matchers {

  val json1 = """
    {
      "name": "Import Shared",
      "namespace": "test.apidoc.import-shared",

      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        }
      }
    }
  """

  val json1File = TestHelper.writeToTempFile(json1)

  val json2 = s"""
    {
      "name": "Import Service",

      "imports": [
	{ "uri": "file://$json1File" }
      ],

      "models": {
        "membership": {
          "fields": [
            { "name": "id", "type": "long" },
            { "name": "user", "type": "test.apidoc.import-shared.user" }
          ]
        }
      }
    }
  """

  it("parses service definition with imports") {
    val validator = ServiceValidator(TestHelper.serviceConfig, json2)
    validator.errors.mkString("") should be("")
  }

}
