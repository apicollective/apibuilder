package core

import org.scalatest.{FunSpec, Matchers}

class ImportSpec extends FunSpec with Matchers {

  describe("with a valid service") {

    val json = """
    {
      "name": "Import Shared",
      "namespace": "test.apidoc.import-shared",
      "key": "import-shared",

      "models": [
        {
          "name": "user",
          "plural": "users",
          "fields": [
            { "name": "id", "type": "long", "required": true }
          ]
        }
      ]
    }
    """

    it("parses service") {
      val path = TestHelper.writeToTempFile(json)
      val service = Import(s"file://$path").service
      service.name should be("Import Shared")
      service.key should be("import-shared")
      service.namespace should be("test.apidoc.import-shared")

      service.models.map(_.name) should be(Seq("user"))

      val user = service.models.find(_.name == "user").get
      user.fields.map(_.name) should be(Seq("id"))
    }
  }
}
