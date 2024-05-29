package db

import anorm._
import db.generated.{MigrationForm, MigrationsDao}
import io.flow.postgresql.Query
import lib.Constants
import play.api.db._

import javax.inject.Inject

/**
 * TODO: Test this class
 * Setup a separate actor to:
 *   - delete if service_version < latest
 *   - attempt migration for num_attempts = 0
 *   - record errors
 */
object Migration {
  val ServiceVersionNumber: String = io.apibuilder.spec.v0.Constants.Version.toLowerCase
}

class InternalMigrationsDao @Inject()(
  @NamedDatabase("default") db: Database,
  migrationsDao: MigrationsDao
) {

  private[this] val VersionsNeedingUpgrade = Query(
    """
      |select v.guid
      |  from versions v
      |  left join migrations m on m.version_guid = v.guid
      | where v.deleted_at is null
      |   and m.id is null
      |   and not exists (
      |    select 1
      |      from cache.services
      |      where services.deleted_at is null
      |        and services.version_guid = v.guid
      |        and services.version = {service_version}
      |  )
      |limit 250
      |""".stripMargin
  ).bind("service_version", Migration.ServiceVersionNumber).withDebugging()

  def queueVersions(): Unit = {
    val versionGuids = db.withConnection { implicit c =>
      VersionsNeedingUpgrade
        .as(SqlParser.get[_root_.java.util.UUID](1).*)
    }
    if (versionGuids.nonEmpty) {
      migrationsDao.insertBatch(Constants.DefaultUserGuid, versionGuids.map { vGuid =>
        MigrationForm(
          versionGuid = vGuid,
          numAttempts = 0,
          errors = None
        )
      })

    }
  }

}
