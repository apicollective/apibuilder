package util

import anorm.SqlParser
import db.Helpers
import io.apibuilder.api.v0.models.{Application, Organization, Version}
import io.flow.postgresql.Query
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.Database

import java.util.UUID

class ProcessDeletesSpec extends PlaySpec with GuiceOneAppPerSuite with Helpers {

  private[this] def processDeletes: ProcessDeletes = app.injector.instanceOf[ProcessDeletes]
  private[this] def database: Database = app.injector.instanceOf[Database]

  private[this] def isDeleted(table: String, guid: UUID): Boolean = {
    database.withConnection { c =>
      Query(s"select case when deleted_at is null then false else true end from $table")
        .equals("guid", guid)
        .as(SqlParser.bool(1).*)(c)
    }.headOption.value
  }

  "organization" in {
    def isAppDeleted(app: Application): Boolean = isDeleted("applications", app.guid)

    def setup(): (Organization, Application) = {
      val org = createOrganization()
      val app = createApplication(org)
      (org, app)
    }

    val (_, app) = setup()
    val (orgDeleted, appDeleted) = setup()
    organizationsDao.softDelete(testUser, orgDeleted)

    isAppDeleted(app) mustBe false
    isAppDeleted(appDeleted) mustBe false

    processDeletes.organizations()
    isAppDeleted(app) mustBe false
    isAppDeleted(appDeleted) mustBe true
  }

  "application" in {
    def isVersionDeleted(version: Version): Boolean = isDeleted("versions", version.guid)

    def setup(): (Application, Version) = {
      val app = createApplication()
      val version = createVersion(app)
      (app, version)
    }

    val (_, version) = setup()
    val (appDeleted, versionDeleted) = setup()

    applicationsDao.softDelete(testUser, appDeleted)

    isVersionDeleted(version) mustBe false
    isVersionDeleted(versionDeleted) mustBe false

    processDeletes.applications()
    isVersionDeleted(version) mustBe false
    isVersionDeleted(versionDeleted) mustBe true
  }
}
