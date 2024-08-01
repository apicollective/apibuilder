package processor

import anorm.SqlParser
import db.{Helpers, InternalApplication, InternalOrganization}
import io.apibuilder.api.v0.models.Organization
import io.flow.postgresql.Query
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.Database

import java.util.UUID

class PurgeDeletedProcessorSpec extends PlaySpec with GuiceOneAppPerSuite with Helpers {

  private def processor: PurgeDeletedProcessor = app.injector.instanceOf[PurgeDeletedProcessor]
  private def database: Database = app.injector.instanceOf[Database]

  private def isDeleted(table: String, guid: UUID): Boolean = {
    database.withConnection { c =>
      Query(s"select count(*) from $table")
        .equals("guid", guid)
        .as(SqlParser.int(1).*)(c)
    }.head == 0
  }

  private def softDelete(table: String, guid: UUID, deletedAt: DateTime): Unit = {
    database.withConnection { c =>
      Query(s"update $table set deleted_at = {deleted_at}::timestamptz, deleted_by_guid = {deleted_by_guid}::uuid")
        .equals("guid", guid)
        .bind("deleted_at", deletedAt)
        .bind("deleted_by_guid", testUser.guid)
        .anormSql()
        .executeUpdate()(c)
    }
    ()
  }

  "organization" in {
    def isAppDeleted(app: InternalApplication): Boolean = isDeleted("applications", app.guid)

    def setup(): (InternalOrganization, InternalApplication) = {
      val org = createOrganization()
      val app = createApplication(org)
      softDelete("applications", app.guid, DateTime.now.minusYears(1))
      (org, app)
    }

    val (_, app) = setup()
    val (orgDeleted, appDeleted) = setup()
    softDelete("organizations", orgDeleted.guid, DateTime.now.minusYears(1))

    isAppDeleted(app) mustBe false
    isAppDeleted(appDeleted) mustBe false

    processor.processRecord(randomString())
    isAppDeleted(app) mustBe true
    isAppDeleted(appDeleted) mustBe true
  }

}
