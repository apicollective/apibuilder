package db

import lib.{DatabaseServiceFetcher, ServiceConfiguration}
import core.VersionMigration
import builder.OriginalValidator
import com.gilt.apidoc.api.v0.models.{Application, Original, OriginalType, Reference, User, Version, VersionForm, Visibility}
import com.gilt.apidoc.internal.v0.models.{TaskDataDiffVersion, TaskDataIndexVersion}
import com.gilt.apidoc.spec.v0.models.Service
import com.gilt.apidoc.spec.v0.models.json._
import lib.VersionTag
import anorm._
import play.api.db._
import play.api.Logger
import play.api.libs.json._
import play.api.Play.current
import java.util.UUID

object VersionsDao {

  private val LatestVersion = "latest"

  private val ServiceVersionNumber: String = com.gilt.apidoc.spec.v0.Constants.Version.toLowerCase

  private val BaseQuery = s"""
    select versions.guid, versions.version,
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

  private val InsertQuery = """
    insert into versions
    (guid, application_guid, version, version_sort_key, created_by_guid)
    values
    ({guid}::uuid, {application_guid}::uuid, {version}, {version_sort_key}, {created_by_guid}::uuid)
  """

  private val DeleteQuery = """
    update versions
       set deleted_at = now(),
           deleted_by_guid = {deleted_by_guid}::uuid
     where deleted_at is null
       and guid = {guid}::uuid
  """

  private val InsertServiceQuery = """
    insert into cache.services
    (guid, version_guid, version, json, created_by_guid)
    values
    ({guid}::uuid, {version_guid}::uuid, {version}, {json}::json, {user_guid}::uuid)
  """

  private val SoftDeleteServiceByVersionGuidAndVersionNumberQuery = """
    update cache.services
       set deleted_at = now(),
           deleted_by_guid = {user_guid}::uuid
     where deleted_at is null
       and version_guid = {version_guid}::uuid
       and version = {version}
  """

  def create(user: User, application: Application, version: String, original: Original, service: Service): Version = {
    val guid = DB.withTransaction { implicit c =>
      doCreate(c, user, application, version, original, service)
    }

    global.Actors.mainActor ! actors.MainActor.Messages.VersionCreated(guid)

    findAll(Authorization.All, guid = Some(guid), limit = 1).headOption.getOrElse {
      sys.error("Failed to create version")
    }
  }

  private def doCreate(
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

    OriginalsDao.create(c, user, guid, original)
    softDeleteService(c, user, guid)
    insertService(c, user, guid, service)

    guid
  }

  def softDelete(deletedBy: User, version: Version) {
    DB.withTransaction { implicit c =>
      softDeleteService(c, deletedBy, version.guid)
      OriginalsDao.softDeleteByVersionGuid(c, deletedBy, version.guid)

      SQL(DeleteQuery).on(
        'guid -> version.guid,
        'deleted_by_guid -> deletedBy.guid
      ).execute()
    }
  }

  def replace(user: User, version: Version, application: Application, original: Original, service: Service): Version = {
    val (versionGuid, taskGuids) = DB.withTransaction { implicit c =>
      softDelete(user, version)
      val versionGuid = doCreate(c, user, application, version.version, original, service)
      val taskGuid = TasksDao.insert(c, user, TaskDataDiffVersion(oldVersionGuid = version.guid, newVersionGuid = versionGuid))
      // TODO: Add task to update search index
      (versionGuid, Seq(taskGuid))
    }

    taskGuids.foreach { taskGuid =>
      global.Actors.mainActor ! actors.MainActor.Messages.TaskCreated(taskGuid)
    }

    findAll(Authorization.All, guid = Some(versionGuid), limit = 1).headOption.getOrElse {
      sys.error(s"Failed to replace version[${version.guid}]")
    }
  }

  def findVersion(authorization: Authorization, orgKey: String, applicationKey: String, version: String): Option[Version] = {
    ApplicationsDao.findByOrganizationKeyAndApplicationKey(authorization, orgKey, applicationKey).flatMap { application =>
      if (version == LatestVersion) {
        VersionsDao.findAll(authorization, applicationGuid = Some(application.guid), limit = 1).headOption
      } else {
        VersionsDao.findByApplicationAndVersion(authorization, application, version)
      }
    }
  }

  def findByApplicationAndVersion(authorization: Authorization, application: Application, version: String): Option[Version] = {
    VersionsDao.findAll(
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
      Some(s"order by versions.version_sort_key desc, versions.created_at desc limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      applicationGuid.map('application_guid -> _.toString),
      version.map('version ->_)
    ).flatten ++ authorization.bindVariables

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
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
          service = service
        )
      }.toSeq
    }
  }

  def migrate(): Map[String, Int] = {
    var good = 0
    var bad = 0

    val sql = BaseQuery.trim + " and versions.deleted_at is null and services.guid is null and originals.data is not null"

    DB.withConnection { implicit c =>
      SQL(sql)().toList.foreach { row =>
        val orgKey = row[String]("organization_key")
        val applicationKey = row[String]("application_key")
        val versionName = row[String]("version")
        val versionGuid = row[UUID]("guid")

        Logger.info(s"Migrating $orgKey/$applicationKey/$versionGuid to version $ServiceVersionNumber")

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
          validator.validate match {
            case Left(errors) => {
            Logger.error(s"Error migrating $orgKey/$applicationKey/$versionName guid[$versionGuid] - invalid JSON: " + errors.distinct.mkString(", "))
              bad += 1
            }
            case Right(service) => {
              insertService(c, UsersDao.AdminUser, versionGuid, service)
              good += 1
            }
          }
        } catch {
          case e: Throwable => {
            Logger.error(s"Error migrating $orgKey/$applicationKey/$versionName guid[$versionGuid] to service versionNumber[$ServiceVersionNumber]: $e")
            bad += 1
          }
        }
      }
    }

    Map(
      "number_migrated" -> good,
      "number_errors" -> bad
    )
  }

  private def softDeleteService(
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

  private def insertService(
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
