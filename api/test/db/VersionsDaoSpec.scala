package db

import lib.{DatabaseServiceFetcher, ServiceConfiguration}
import builder.OriginalValidator
import io.apibuilder.api.v0.models.OriginalType
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import java.util.UUID
import play.api.libs.json.Json

class VersionsDaoSpec extends PlaySpec with OneAppPerSuite with db.Helpers {

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
    createApplication(
      org = testOrg,
      form = createApplicationForm().copy(key = Some(key))
    )
  }

  "with an application" must {

    val applicationKey = "test-" + UUID.randomUUID.toString
    val application: io.apibuilder.api.v0.models.Application = createApplication(applicationKey)
    val service = createService(application)

    "create" in {
      val version = versionsDao.create(createdBy, application, "1.0.0", Original, service)
      createVersion().version must be("1.0.0")
    }

    "findByApplicationAndVersion" in {
      versionsDao.create(createdBy, application, "1.0.1", Original, service)
      versionsDao.findByApplicationAndVersion(Authorization.All, application, "1.0.1").map(_.service) must be(Some(service))
    }

    "soft delete" in {
      val version1 = versionsDao.create(createdBy, application, "1.0.2", Original, service)
      versionsDao.softDelete(createdBy, version1)

      val version2 = versionsDao.create(createdBy, application, "1.0.2", Original, service)
      version2.copy(
        guid = version1.guid,
        audit = version1.audit
      ) must be(version1)
      version1.guid != version2.guid must be(true)
    }

  }

  "sorts properly" in {
    val app = createApplication()
    val service = createService(app)
    versionsDao.create(createdBy, app, "1.0.2", Original, service)
    versionsDao.create(createdBy, app, "1.0.2-dev", Original, service)

    versionsDao.findAll(
      Authorization.All,
      applicationGuid = Some(app.guid)
    ).map(_.version) must be(Seq("1.0.2", "1.0.2-dev"))
  }

  "can parse original" in {
    val app = createApplication()
    val service = createService(app)
    val version = versionsDao.create(createdBy, app, "1.0.2", Original, service)

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

  "trims version number" in {
    val app = createApplication()
    val service = createService(app)
    val version = versionsDao.create(createdBy, app, " 1.0.2\n ", Original, service)
    version.version must be("1.0.2")
  }

  "findAllVersions" in {
    val app = createApplication()
    val service = createService(app)
    versionsDao.create(createdBy, app, "1.0.1", Original, service)
    versionsDao.create(createdBy, app, "1.0.2", Original, service)

    versionsDao.findAllVersions(
      Authorization.All,
      applicationGuid = Some(app.guid)
    ).map(_.version) must be(Seq("1.0.2", "1.0.1"))
  }
}
