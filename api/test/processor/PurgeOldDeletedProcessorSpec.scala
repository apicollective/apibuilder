package processor

import anorm.SqlParser
import db.Helpers
import io.apibuilder.api.v0.models.{Application, Organization}
import io.flow.postgresql.Query
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.Database

import java.util.UUID

class PurgeOldDeletedProcessorSpec extends PlaySpec with GuiceOneAppPerSuite with Helpers {

  private[this] def processor: CleanupDeletionsProcessor = app.injector.instanceOf[CleanupDeletionsProcessor]
  private[this] def database: Database = app.injector.instanceOf[Database]

  private[this] def isDeleted(table: String, guid: UUID): Boolean = {
    database.withConnection { c =>
      Query(s"select count(*) from $table")
        .equals("guid", guid)
        .as(SqlParser.int(1).*)(c)
    }.headOption.exists(_ > 0)
  }

  private[this] def softDelete(table: String, guid: UUID): Unit = {
    database.withConnection { c =>
      Query(s"update $table set deleted_at = now() - interval '1 year'")
        .equals("guid", guid)
        .anormSql()
        .executeUpdate()(c)
    }
    ()
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
    softDelete("organizations", orgDeleted.guid)

    isAppDeleted(app) mustBe false
    isAppDeleted(appDeleted) mustBe false

    processor.organizations()
    isAppDeleted(app) mustBe false
    isAppDeleted(appDeleted) mustBe true
  }

}
