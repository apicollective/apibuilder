package core

import com.bryzek.apidoc.spec.v0.models.{Application, Organization, Import}
import org.scalatest.{FunSpec, Matchers}

class ImporterSpec extends FunSpec with Matchers {

  describe("with an invalid service") {
    val json = """
    {
      "name": "Import Shared",
      "apidoc": { "version": "0.9.6" }
    }
    """

    val path = TestHelper.writeToTempFile(json)
    val imp = Importer(FileServiceFetcher(), s"file://$path")
    imp.validate.size should be > 0
  }

  describe("with a valid service") {

    val json = """
    {
      "name": "Import Shared",
      "apidoc": { "version": "0.9.6" },
      "organization": { "key": "test" },
      "application": { "key": "import-shared" },
      "namespace": "test.apidoc.import-shared",
      "version": "1.0.0",
      "attributes": [],

      "imports": [],
      "headers": [],
      "unions": [],
      "enums": [],
      "resources": [],
      "info": {},

      "models": [
        {
          "name": "user",
          "plural": "users",
          "fields": [
            { "name": "id", "type": "long", "required": true, "attributes": [] }
          ],
          "attributes": []
        }
      ]
    }
    """

    it("parses service") {
      val path = TestHelper.writeToTempFile(json)
      val imp = Importer(FileServiceFetcher(), s"file://$path")
      imp.validate should be(Seq.empty)

      val service = imp.service
      service.name should be("Import Shared")
      service.organization should be(Organization(key = "test"))
      service.application should be(Application(key = "import-shared"))
      service.namespace should be("test.apidoc.import-shared")

      service.models.map(_.name) should be(Seq("user"))

      val user = service.models.find(_.name == "user").get
      user.fields.map(_.name) should be(Seq("id"))
    }
  }
}
