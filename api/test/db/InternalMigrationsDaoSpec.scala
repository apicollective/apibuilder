package db

import db.generated.MigrationsDao
import io.flow.postgresql.Query
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class InternalMigrationsDaoSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers with DbUtils {

  private[this] def internalMigrationsDao: InternalMigrationsDao = injector.instanceOf[InternalMigrationsDao]
  private[this] def generatedMigrationsDao: MigrationsDao = injector.instanceOf[MigrationsDao]

  "queueVersions" in {
    val version = createVersion()
    println(s"Created version: ${version.guid}")
    def count = generatedMigrationsDao.findAll(versionGuid = Some(version.guid), limit = None).length

    execute(
      Query("update cache.services set deleted_at=now(), deleted_by_guid={user_guid}::uuid")
        .equals("version_guid", version.guid)
        .bind("user_guid", testUser.guid)
    )

    count mustBe 0
    internalMigrationsDao.queueVersions()
    count mustBe 1
  }

}