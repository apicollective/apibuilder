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

  "have all child tables" must {
    def getTablesSoft(columnName: String): Seq[String] = {
      database.withConnection { c =>
        Query(
          """
            |select c1.table_schema || '.' || c1.table_name
            |  from information_schema.columns c1
            |  join information_schema.columns c2 on c2.table_schema = c1.table_schema and c2.table_name = c1.table_name and c2.column_name = 'deleted_by_guid'
            | where c1.column_name = {column_name}
            | order by 1
            |""".stripMargin
        )
          .bind("column_name", columnName)
          .as(SqlParser.str(1).*)(c)
      }
    }

    def getTablesHard(columnName: String): Seq[String] = {
      database.withConnection { c =>
        Query(
          """
            |select c1.table_schema || '.' || c1.table_name
            |  from information_schema.columns c1
            |  left join information_schema.columns c2 on c2.table_schema = c1.table_schema and c2.table_name = c1.table_name and c2.column_name = 'deleted_by_guid'
            | where c1.column_name = {column_name}
            |   and c2.table_name is null
            | order by 1
            |""".stripMargin
        )
          .bind("column_name", columnName)
          .as(SqlParser.str(1).*)(c)
      }
    }

    "Organization" must {
      "soft" in {
        getTablesSoft("organization_guid") mustBe ProcessDeletes.OrganizationSoft
      }
      "hard" in {
        getTablesHard("organization_guid") mustBe ProcessDeletes.OrganizationHard
      }
    }

    "Application" must {
      "soft" in {
        val Ignore = Seq("public.application_moves")
        getTablesSoft("application_guid")
          .filterNot(Ignore.contains) mustBe ProcessDeletes.ApplicationSoft
      }
      "hard" in {
        getTablesHard("application_guid") mustBe ProcessDeletes.ApplicationHard
      }
    }

    "Version" must {
      "soft" in {
        getTablesSoft("version_guid") mustBe ProcessDeletes.VersionSoft
      }
      "hard" in {
        getTablesHard("version_guid") mustBe ProcessDeletes.VersionHard
      }
    }
  }
}
