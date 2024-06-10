package processor

import anorm.SqlParser
import db.Helpers
import io.apibuilder.api.v0.models.{Application, Organization, Version}
import io.flow.postgresql.Query
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.Database

import java.util.UUID

class CleanupDeletionsProcessorSpec extends PlaySpec with GuiceOneAppPerSuite with Helpers {

  private[this] def processor: CleanupDeletionsProcessor = app.injector.instanceOf[CleanupDeletionsProcessor]
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

    processor.organizations()
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

    processor.applications()
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
        getTablesSoft("organization_guid") mustBe DeleteMetadata.OrganizationSoft.map(_.name)
      }
      "hard" in {
        getTablesHard("organization_guid") mustBe DeleteMetadata.OrganizationHard.map(_.name)
      }
    }

    "Application" must {
      "soft" in {
        getTablesSoft("application_guid") mustBe DeleteMetadata.ApplicationSoft.map(_.name)
      }
      "hard" in {
        getTablesHard("application_guid") mustBe DeleteMetadata.ApplicationHard.map(_.name)
      }
    }

    "Version" must {
      "soft" in {
        getTablesSoft("version_guid") mustBe DeleteMetadata.VersionSoft.map(_.name)
      }
      "hard" in {
        getTablesHard("version_guid") mustBe DeleteMetadata.VersionHard.map(_.name)
      }
    }
  }
}
