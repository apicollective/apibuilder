package db

import anorm._
import builder.OriginalValidator
import builder.api_json.upgrades.ServiceParser
import cats.data.Validated.{Invalid, Valid}
import core.{ServiceFetcher, VersionMigration}
import db.generated.MigrationsDao
import io.apibuilder.api.v0.models._
import io.apibuilder.internal.v0.models.{TaskDataDiffVersion, TaskDataIndexApplication}
import io.apibuilder.spec.v0.models.Service
import io.apibuilder.spec.v0.models.json._
import io.flow.postgresql.Query
import lib.{Constants, ServiceConfiguration, ServiceUri, ValidatedHelpers, VersionTag}
import play.api.Logger
import play.api.db._
import play.api.libs.json._

import java.util.UUID
import javax.inject.{Inject, Named}
import scala.annotation.tailrec

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
      |  join originals on originals.version_guid = v.guid and originals.deleted_at is null
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
  ).bind("service_version", Migration.ServiceVersionNumber)

  def queueVersions(): Unit = {
    val versionGuids = db.withConnection { implicit c =>
      VersionsNeedingUpgrade
        .as(SqlParser.get[_root_.java.util.UUID](1).*)
    }
    if (versionGuids.nonEmpty) {
      migrationsDao.insertBatch(Constants.DefaultUserGuid)

    }
  }

}
