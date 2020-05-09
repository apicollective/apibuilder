package core

import io.apibuilder.spec.v0.models.{Application, Organization}
import io.apibuilder.spec.v0.models.json._
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.Json

class ImporterSpec extends FunSpec with Matchers with helpers.ServiceHelpers {

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
    val originalService = makeService(
      name = "Import Shared",
      organization = makeOrganization(key = "test"),
      application = makeApplication(key = "import-shared"),
      namespace = "test.apibuilder.import-shared",
      models = Seq(makeModel(
        name = "user",
        fields = Seq(makeField(name = "id", `type` = "long")),
      )),
    )

    it("parses service") {
      val path = TestHelper.writeToTempFile(Json.toJson(originalService).toString)
      val imp = Importer(FileServiceFetcher(), s"file://$path")
      imp.validate should be(Nil)

      val service = imp.service
      service.name should be("Import Shared")
      service.organization should be(Organization(key = "test"))
      service.application should be(Application(key = "import-shared"))
      service.namespace should be("test.apibuilder.import-shared")

      service.models.map(_.name) should be(Seq("user"))

      val user = service.models.find(_.name == "user").get
      user.fields.map(_.name) should be(Seq("id"))

      imp.service should equal(originalService)
    }
  }
}
