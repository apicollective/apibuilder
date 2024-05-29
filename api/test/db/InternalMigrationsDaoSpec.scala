package db

import db.generated.MigrationsDao
import io.flow.postgresql.Query
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import java.util.UUID

class InternalMigrationsDaoSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers with DbUtils {

  private[this] def internalMigrationsDao: InternalMigrationsDao = injector.instanceOf[InternalMigrationsDao]
  private[this] def generatedMigrationsDao: MigrationsDao = injector.instanceOf[MigrationsDao]

  private[this] def deleteCachedServices(versionGuid: UUID): Unit = {
    execute(
      Query("update cache.services set deleted_at=now(), deleted_by_guid={user_guid}::uuid")
        .equals("version_guid", versionGuid)
        .bind("user_guid", testUser.guid)
    )
  }

  "queueVersions" in {
    val version = createVersion()
    deleteCachedServices(version.guid)
    def count = generatedMigrationsDao.findAll(versionGuid = Some(version.guid), limit = None).length

    count mustBe 0
    internalMigrationsDao.queueVersions()
    count mustBe 1
  }

  "migrateBatch" in {
    val version = createVersion()
    deleteCachedServices(version.guid)

    def exists = versionsDao.findByGuid(Authorization.All, version.guid).isDefined

    exists mustBe false

    internalMigrationsDao.queueVersions()
    internalMigrationsDao.migrateBatch(1000L)

    exists mustBe true
  }
}