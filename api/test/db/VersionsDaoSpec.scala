package db

import lib.{DatabaseServiceFetcher, ServiceConfiguration}
import builder.OriginalValidator
import io.apibuilder.api.v0.models.{ApplicationForm, OriginalType, Version, Visibility}
import io.apibuilder.spec.v0.models.{Application, Organization, Service}
import io.apibuilder.spec.v0.models.json._
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID
import play.api.libs.json.{Json, JsObject}

class VersionsDaoSpec extends FunSpec with Matchers with util.TestApplication {

  private[this] val Original = io.apibuilder.api.v0.models.Original(
    `type` = OriginalType.ApiJson,
    data = Json.obj(
      "apidoc" -> Json.obj(
        "version" -> io.apibuilder.spec.v0.Constants.Version
      ),
      "name" -> s"test-${UUID.randomUUID}"
    ).toString
  )

  private[this] def createApplication(key: String = "test-" + UUID.randomUUID.toString): io.apibuilder.api.v0.models.Application = {
    Util.createApplication(
      org = Util.testOrg,
      form = Util.createApplicationForm().copy(key = Some(key))
    )
  }

  describe("with an application") {

    val applicationKey = "test-" + UUID.randomUUID.toString
    val application: io.apibuilder.api.v0.models.Application = createApplication(applicationKey)
    val service = Util.createService(application)

    it("create") {
      val version = versionsDao.create(Util.createdBy, application, "1.0.0", Original, service)
      Util.createVersion().version should be("1.0.0")
    }

    it("findByApplicationAndVersion") {
      versionsDao.create(Util.createdBy, application, "1.0.1", Original, service)
      versionsDao.findByApplicationAndVersion(Authorization.All, application, "1.0.1").map(_.service) should be(Some(service))
    }

    it("soft delete") {
      val version1 = versionsDao.create(Util.createdBy, application, "1.0.2", Original, service)
      versionsDao.softDelete(Util.createdBy, version1)

      val version2 = versionsDao.create(Util.createdBy, application, "1.0.2", Original, service)
      version2.copy(
        guid = version1.guid,
        audit = version1.audit
      ) should be(version1)
      version2.guid shouldNot be(version1.guid)
    }

  }

  it("sorts properly") {
    val app = createApplication()
    val service = Util.createService(app)
    versionsDao.create(Util.createdBy, app, "1.0.2", Original, service)
    versionsDao.create(Util.createdBy, app, "1.0.2-dev", Original, service)

    versionsDao.findAll(
      Authorization.All,
      applicationGuid = Some(app.guid)
    ).map(_.version) should be(Seq("1.0.2", "1.0.2-dev"))
  }

  it("can parse original") {
    val app = createApplication()
    val service = Util.createService(app)
    val version = versionsDao.create(Util.createdBy, app, "1.0.2", Original, service)

    val serviceConfig = ServiceConfiguration(
      orgKey = "test",
      orgNamespace = "test.apibuilder",
      version = "0.0.2"
    )

    val validator = OriginalValidator(
      config = serviceConfig,
      original = version.original.getOrElse {
        sys.error("Missing original")
      },
      fetcher = DatabaseServiceFetcher(Authorization.All)
    )
    validator.validate() match {
      case Left(errors) => fail(errors.mkString("\n"))
      case Right(_) => {}
    }
  }

  it("trims version number") {
    val app = createApplication()
    val service = Util.createService(app)
    val version = versionsDao.create(Util.createdBy, app, " 1.0.2\n ", Original, service)
    version.version should be("1.0.2")
  }

  it("findAllVersions") {
    val app = createApplication()
    val service = Util.createService(app)
    versionsDao.create(Util.createdBy, app, "1.0.1", Original, service)
    versionsDao.create(Util.createdBy, app, "1.0.2", Original, service)

    versionsDao.findAllVersions(
      Authorization.All,
      applicationGuid = Some(app.guid)
    ).map(_.version) should be(Seq("1.0.2", "1.0.1"))
  }
}
