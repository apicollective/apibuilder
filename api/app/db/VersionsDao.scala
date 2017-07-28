package db

import lib.{DatabaseServiceFetcher, ServiceConfiguration}
import core.VersionMigration
import builder.OriginalValidator
import io.apibuilder.api.v0.models.{Application, Original, OriginalType, User, Version, VersionForm, Visibility}
import io.apibuilder.common.v0.models.Reference
import io.apibuilder.internal.v0.models.{TaskDataDiffVersion, TaskDataIndexApplication}
import io.apibuilder.spec.v0.models.Service
import io.apibuilder.spec.v0.models.json._
import lib.VersionTag
import anorm._
import javax.inject.{Inject, Named, Singleton}

import play.api.db._
import play.api.Logger
import play.api.libs.json._
import play.api.Play.current
import java.util.UUID

import scala.annotation.tailrec

case class MigrationStats(good: Long, bad: Long)

@Singleton
class VersionsDao @Inject() (
  @Named("main-actor") mainActor: akka.actor.ActorRef,
  applicationsDao: ApplicationsDao,
  originalsDao: OriginalsDao,
  tasksDao: TasksDao,
  usersDao: UsersDao
) {

  private[this] val LatestVersion = "latest"

  private[this] val ServiceVersionNumber: String = io.apibuilder.spec.v0.Constants.Version.toLowerCase

  private[this] val BaseQuery = s"""
    select versions.guid, versions.version,
           ${AuditsDao.queryCreation("versions")},
           originals.type as original_type,
           originals.data as original_data,
           organizations.guid as organization_guid,
           organizations.key as organization_key,
           organizations.namespace as organization_namespace,
           applications.guid as application_guid,
           applications.key as application_key,
           services.json::varchar as service_json
     from versions
     left join cache.services on services.deleted_at is null and services.version_guid = versions.guid and services.version = '$ServiceVersionNumber'
     left join originals on originals.version_guid = versions.guid and originals.deleted_at is null
     join applications on applications.deleted_at is null and applications.guid = versions.application_guid
     join organizations on organizations.deleted_at is null and organizations.guid = applications.organization_guid
    where true
  """

  private[this] val InsertQuery = """
    insert into versions
    (guid, application_guid, version, version_sort_key, created_by_guid)
    values
    ({guid}::uuid, {application_guid}::uuid, {version}, {version_sort_key}, {created_by_guid}::uuid)
  """

  private[this] val DeleteQuery = """
    update versions
       set deleted_at = now(),
           deleted_by_guid = {deleted_by_guid}::uuid
     where deleted_at is null
       and guid = {guid}::uuid
  """

  private[this] val InsertServiceQuery = """
    insert into cache.services
    (guid, version_guid, version, json, created_by_guid)
    values
    ({guid}::uuid, {version_guid}::uuid, {version}, {json}::json, {user_guid}::uuid)
  """

  private[this] val SoftDeleteServiceByVersionGuidAndVersionNumberQuery = """
    update cache.services
       set deleted_at = now(),
           deleted_by_guid = {user_guid}::uuid
     where deleted_at is null
       and version_guid = {version_guid}::uuid
       and version = {version}
  """

  def create(user: User, application: Application, version: String, original: Original, service: Service): Version = {
    val latestVersion: Option[Version] = findAll(
      Authorization.User(user.guid),
      applicationGuid = Some(application.guid),
      limit = 1
    ).headOption

    val (guid, taskGuid) = DB.withTransaction { implicit c =>
      val versionGuid = doCreate(c, user, application, version, original, service)
      val taskGuid = latestVersion.map { v =>
        createDiffTask(user, v.guid, versionGuid)
      }
      (versionGuid, taskGuid)
    }

    taskGuid.map { guid =>
      mainActor ! actors.MainActor.Messages.TaskCreated(guid)
    }

    findAll(Authorization.All, guid = Some(guid), limit = 1).headOption.getOrElse {
      sys.error("Failed to create version")
    }
  }

  private[this] def doCreate(
    implicit c: java.sql.Connection,
    user: User,
    application: Application,
    version: String,
    original: Original,
    service: Service
  ): UUID = {
    val guid = UUID.randomUUID

    SQL(InsertQuery).on(
      'guid -> guid,
      'application_guid -> application.guid,
      'version -> version.trim,
      'version_sort_key -> VersionTag(version.trim).sortKey,
      'created_by_guid -> user.guid
    ).execute()

    originalsDao.create(c, user, guid, original)
    softDeleteService(c, user, guid)
    insertService(c, user, guid, service)

    guid
  }

  def softDelete(deletedBy: User, version: Version) {
    DB.withTransaction { implicit c =>
      softDeleteService(c, deletedBy, version.guid)
      originalsDao.softDeleteByVersionGuid(c, deletedBy, version.guid)

      SQL(DeleteQuery).on(
        'guid -> version.guid,
        'deleted_by_guid -> deletedBy.guid
      ).execute()
    }
  }

  private[this] def createDiffTask(
    user: User, oldVersionGuid: UUID, newVersionGuid: UUID
  ) (
    implicit c: java.sql.Connection
  ): UUID = {
    tasksDao.insert(c, user, TaskDataDiffVersion(oldVersionGuid = oldVersionGuid, newVersionGuid = newVersionGuid))
  }

  def replace(user: User, version: Version, application: Application, original: Original, service: Service): Version = {
    val (versionGuid, taskGuids) = DB.withTransaction { implicit c =>
      softDelete(user, version)
      val versionGuid = doCreate(c, user, application, version.version, original, service)
      val diffTaskGuid = createDiffTask(user, version.guid, versionGuid)
      val indexTaskGuid = tasksDao.insert(c, user, TaskDataIndexApplication(application.guid))
      (versionGuid, Seq(diffTaskGuid, indexTaskGuid))
    }

    taskGuids.foreach { taskGuid =>
      mainActor ! actors.MainActor.Messages.TaskCreated(taskGuid)
    }

    findAll(Authorization.All, guid = Some(versionGuid), limit = 1).headOption.getOrElse {
      sys.error(s"Failed to replace version[${version.guid}]")
    }
  }

  def findVersion(
    authorization: Authorization,
    orgKey: String,
    applicationKey: String,
    version: String
  ): Option[Version] = {
    applicationsDao.findByOrganizationKeyAndApplicationKey(authorization, orgKey, applicationKey).flatMap { application =>
      if (version == LatestVersion) {
        findAll(authorization, applicationGuid = Some(application.guid), limit = 1).headOption
      } else {
        findByApplicationAndVersion(authorization, application, version)
      }
    }
  }

  def findByApplicationAndVersion(authorization: Authorization, application: Application, version: String): Option[Version] = {
    findAll(
      authorization,
      applicationGuid = Some(application.guid),
      version = Some(version),
      limit = 1
    ).headOption
  }

  def findByGuid(
    authorization: Authorization,
    guid: UUID,
    isDeleted: Option[Boolean] = Some(false)
  ): Option[Version] = {
    findAll(authorization, guid = Some(guid), isDeleted = isDeleted, limit = 1).headOption
  }

  def findAll(
    authorization: Authorization,
    applicationGuid: Option[UUID] = None,
    guid: Option[UUID] = None,
    version: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Version] = {
    val sql = Seq(
      Some(BaseQuery.trim + " and services.guid is not null "),
      authorization.applicationFilter().map(v => "and " + v),
      guid.map { v => "and versions.guid = {guid}::uuid" },
      applicationGuid.map { _ => "and versions.application_guid = {application_guid}::uuid" },
      version.map { v => "and versions.version = {version}" },
      isDeleted.map(Filters.isDeleted("versions", _)),
      Some(s"order by versions.version_sort_key desc, versions.created_at desc limit $limit offset $offset")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      applicationGuid.map('application_guid -> _.toString),
      version.map('version ->_)
    ).flatten ++ authorization.bindVariables

    DB.withConnection { implicit c =>
      sys.error("TODO PARSER") /*SQL(sql).on(bind: _*)().toList.map { row =>
        val version = row[String]("version")

        val original = row[Option[String]]("original_data").map { data =>
          Original(
            `type` = OriginalType(row[String]("original_type")),
              data = data
          )
        }

        val service = Json.parse(row[String]("service_json")).as[Service]

        Version(
          guid = row[UUID]("guid"),
          organization = Reference(
            guid = row[UUID]("organization_guid"),
            key = row[String]("organization_key")
          ),
          application = Reference(
            guid = row[UUID]("application_guid"),
            key = row[String]("application_key")
          ),
          version = version,
          original = original,
          service = service,
          audit = AuditsDao.fromRowCreation(row)
        )
                                }*/
    }
  }

  /**
    * Upgrades all versions to the latest API Builder spec in multiple
    * passes until we have either upgraded all of them or all
    * remaining versions cannot be upgraded.
    */
  def migrate(): MigrationStats = {
    var stats = migrateSingleRun()
    var totalRecords = stats.good + stats.bad
    var totalGood = stats.good

    while (stats.good > 0) {
      Logger.info(s"migrate() interim statistics: ${stats}")
      stats = migrateSingleRun()
      totalGood += stats.good
    }

    val finalStats = MigrationStats(good = totalGood, bad = totalRecords - totalGood)
    Logger.info(s"migrate() finished: good[${finalStats.good}] bad[${finalStats.bad}]")
    finalStats
  }

//  @tailrec
  private[this] def migrateSingleRun(limit: Int = 5000, offset: Int = 0, stats: MigrationStats = MigrationStats(good = 0, bad = 0)): MigrationStats = {
    sys.error("TODO PARSER")
    /*
    var good = 0l
    var bad = 0l

    val sql = BaseQuery.trim + s" and versions.deleted_at is null and services.guid is null and originals.data is not null order by versions.created_at desc limit $limit offset $offset"

    val processed = DB.withConnection { implicit c =>
      val records = SQL(sql)().toList
      records.foreach { row =>
        val orgKey = row[String]("organization_key")
        val applicationKey = row[String]("application_key")
        val versionName = row[String]("version")
        val versionGuid = row[UUID]("guid")

        Logger.info(s"Migrating $orgKey/$applicationKey/$versionName versionGuid[$versionGuid] to latest API Builder spec version[$ServiceVersionNumber]")

        val config = ServiceConfiguration(
          orgKey = orgKey,
          orgNamespace = row[String]("organization_namespace"),
          version = row[String]("version")
        )

        val original = Original(
          `type` = OriginalType(row[String]("original_type")),
          data = row[String]("original_data")
        )

        try {
          val validator = OriginalValidator(
            config = config,
            original = original,
            fetcher = DatabaseServiceFetcher(Authorization.All),
            migration = VersionMigration(internal = true)
          )
          validator.validate() match {
            case Left(errors) => {
              Logger.error(s"Error migrating $orgKey/$applicationKey/$versionName versionGuid[$versionGuid] - invalid JSON: " + errors.distinct.mkString(", "))
              bad += 1
            }
            case Right(service) => {
              insertService(c, usersDao.AdminUser, versionGuid, service)
              good += 1
            }
          }
        } catch {
          case e: Throwable => {
            e.printStackTrace(System.err)
            Logger.error(s"Error migrating $orgKey/$applicationKey/$versionName versionGuid[$versionGuid]: $e")
            bad += 1
          }
        }
      }
      records
    }

    val finalStats = MigrationStats(good = stats.good + good, bad = stats.bad + bad)

    if (processed.size < limit) {
      finalStats
    } else {
      migrateSingleRun(limit, offset + limit, finalStats)
    }
         */
  }

  private[this] def softDeleteService(
    implicit c: java.sql.Connection,
    user: User,
    versionGuid: UUID
  ) {
    SQL(SoftDeleteServiceByVersionGuidAndVersionNumberQuery).on(
      'version_guid -> versionGuid,
      'version -> ServiceVersionNumber,
      'user_guid -> user.guid
    )
  }

  private[this] def insertService(
    implicit c: java.sql.Connection,
    user: User,
    versionGuid: UUID,
    service: Service
  ) {
    SQL(InsertServiceQuery).on(
      'guid -> UUID.randomUUID,
      'version_guid -> versionGuid,
      'version -> ServiceVersionNumber,
      'json -> Json.toJson(service).as[JsObject].toString.trim,
      'user_guid -> user.guid
    ).execute()
  }
}
