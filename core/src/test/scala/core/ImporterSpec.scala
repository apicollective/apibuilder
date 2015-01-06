package core

import com.gilt.apidocspec.models.Import
import org.scalatest.{FunSpec, Matchers}

class ImporterSpec extends FunSpec with Matchers {

  describe("with an invalid service") {
    val json = """
    {
      "name": "Import Shared"
    }
    """

    val path = TestHelper.writeToTempFile(json)
    val imp = Importer(Import(s"file://$path"))
    imp.validate.size should be > 0
  }

  describe("with a valid service") {

    val json = """
    {
      "name": "Import Shared",
      "namespace": "test.apidoc.import-shared",
      "key": "import-shared",
      "version": "1.0.0",

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
      val imp = Importer(Import(s"file://$path"))
      imp.validate should be(Seq.empty)

      val service = imp.service
      service.name should be("Import Shared")
      service.key should be("import-shared")
      service.namespace should be("test.apidoc.import-shared")

      service.models.map(_.name) should be(Seq("user"))

      val user = service.models.find(_.name == "user").get
      user.fields.map(_.name) should be(Seq("id"))
    }
  }
}
