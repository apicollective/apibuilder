package db

import java.util.UUID
import builder.OriginalValidator
import helpers.ValidatedTestHelpers
import lib.ServiceConfiguration
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class VersionsDaoSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers with ValidatedTestHelpers {

  private val Original = createOriginal()

  "with an application" must {

    val applicationKey = "test-" + UUID.randomUUID.toString
    lazy val application: InternalApplication = createApplicationByKey(key = applicationKey)
    lazy val service = createService(application)

    "create" in {
      val v = versionsModel.toModel(
        versionsDao.create(testUser, application, "1.0.0", Original, service)
      ).get
      v.version must be("1.0.0")
      v.service.namespace must be(service.namespace)
      createVersion().version must be("1.0.0")
    }

    "findByApplicationAndVersion" in {
      versionsDao.create(testUser, application, "1.0.1", Original, service)
      val svc = versionsDao.findByApplicationAndVersion(Authorization.All, application, "1.0.1")
        .flatMap(versionsModel.toModel)
        .get.service
      svc.namespace must be(service.namespace)
    }

    "soft delete" in {
      val version1 = versionsDao.create(testUser, application, "1.0.2", Original, service)
      versionsDao.softDelete(testUser, version1)

      val version2 = versionsDao.create(testUser, application, "1.0.2", Original, service)
      version2.copy(
        guid = version1.guid,
        audit = version1.audit
      ) must be(version1)
      version1.guid != version2.guid must be(true)
    }

  }

  "sorts properly" in {
    val app = createApplicationByKey()
    val service = createService(app)
    versionsDao.create(testUser, app, "1.0.2", Original, service)
    versionsDao.create(testUser, app, "1.0.2-dev", Original, service)

    versionsDao.findAll(
      Authorization.All,
      applicationGuid = Some(app.guid)
    ).map(_.version) must be(Seq("1.0.2", "1.0.2-dev"))
  }

  "can parse original" in {
    val app = createApplicationByKey(testOrg)
    val service = createService(app)
    val original = createOriginal(service)
    versionsDao.create(testUser, app, "1.0.2", original, service)

    val serviceConfig = ServiceConfiguration(
      orgKey = testOrg.key,
      orgNamespace = testOrg.db.namespace,
      version = "0.0.2"
    )

    val validator = OriginalValidator(
      config = serviceConfig,
      `type` = original.`type`,
      fetcher = databaseServiceFetcher.instance(Authorization.All)
    )

    val svc = expectValid {
      validator.validate(original.data)
    }
    svc.name must be(service.name)
    svc.namespace must be(serviceConfig.applicationNamespace(svc.name))
  }

  "trims version number" in {
    val app = createApplicationByKey()
    val service = createService(app)
    val version = versionsDao.create(testUser, app, " 1.0.2\n ", Original, service)
    version.version must be("1.0.2")
  }

  "findAllVersions" in {
    val app = createApplicationByKey()
    val service = createService(app)
    versionsDao.create(testUser, app, "1.0.1", Original, service)
    versionsDao.create(testUser, app, "1.0.2", Original, service)

    versionsDao.findAllVersions(
      Authorization.All,
      applicationGuid = Some(app.guid)
    ).map(_.version) must be(Seq("1.0.2", "1.0.1"))
  }

  "findVersion" in {
    val app = createApplicationByKey(testOrg)
    val service = createService(app)

    //create 1.0.0 to 3.9.9
    for (p <- 1 to 3) { for (q <- 0 to 9) { for (r <- 0 to 9) { versionsDao.create(testUser, app, s"${p}.${q}.${r}", Original, service) } } }

    //Latest
    versionsDao.findVersion(
      Authorization.All,
      orgKey = testOrg.key,
      applicationKey = app.key,
      version = "latest"
    ).map(_.version) must be(Some("3.9.9"))

    // Sem Ver ~ operator. Should specifies minimum, and lets least significant digit go up

    versionsDao.findVersion(
      Authorization.All,
      orgKey = testOrg.key,
      applicationKey = app.key,
      version = "~1.0"
    ).map(_.version) must be(Some("1.9.9"))

    versionsDao.findVersion(
      Authorization.All,
      orgKey = testOrg.key,
      applicationKey = app.key,
      version = "~1.2"
    ).map(_.version) must be(Some("1.9.9"))

    versionsDao.findVersion(
      Authorization.All,
      orgKey = testOrg.key,
      applicationKey = app.key,
      version = "~1.2.3"
    ).map(_.version) must be(Some("1.2.9"))

    versionsDao.findVersion(
      Authorization.All,
      orgKey = testOrg.key,
      applicationKey = app.key,
      version = "~2.0"
    ).map(_.version) must be(Some("2.9.9"))

    versionsDao.findVersion(
      Authorization.All,
      orgKey = testOrg.key,
      applicationKey = app.key,
      version = "~1"
    ).map(_.version) must be(Some("3.9.9")) //collapses to 'latest'

    versionsDao.findVersion(
      Authorization.All,
      orgKey = testOrg.key,
      applicationKey = app.key,
      version = "~4.2"
    ).map(_.version) must be(None)

    versionsDao.findVersion(
      Authorization.All,
      orgKey = testOrg.key,
      applicationKey = app.key,
      version = "~4.1.3"
    ).map(_.version) must be(None)

  }
}
