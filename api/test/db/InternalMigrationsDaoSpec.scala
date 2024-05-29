package db

import db.generated.MigrationsDao
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.Database

class InternalMigrationsDaoSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers {

  private[this] val db: Database = injector.instanceOf[Database]
  private[this] def internalMigrationsDao: InternalMigrationsDao = injector.instanceOf[InternalMigrationsDao]
  private[this] def generatedMigrationsDao: MigrationsDao = injector.instanceOf[MigrationsDao]

  "foo" in {
    val version = createVersion()
    println(s"Created version: ${version.guid}")
    def count = generatedMigrationsDao.findAll(versionGuid = Some(version.guid), limit = None).length

    count mustBe 0
    internalMigrationsDao.queueVersions()
    count mustBe 1
  }

}