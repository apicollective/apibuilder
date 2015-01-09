package db

import core.ServiceConfiguration
import com.gilt.apidoc.models.{Application, Reference, User, Version, VersionForm, Visibility}
import com.gilt.apidocspec.models.Service
import com.gilt.apidocspec.models.json._
import lib.VersionTag
import anorm._
import play.api.db._
import play.api.Logger
import play.api.libs.json._
import play.api.Play.current
import java.util.UUID

object VersionsDao {

  private val LatestVersion = "latest"

  private val ServiceVersionNumber = 1

  private val BaseQuery = s"""
    select versions.guid, versions.version, versions.original, versions.old_json::varchar,
           organizations.guid as organization_guid,
           organizations.key as organization_key,
           organizations.namespace as organization_namespace,
           applications.guid as application_guid,
           applications.key as application_key,
           services.json::varchar as service_json
     from versions
     left join cache.services on services.deleted_at is null and services.version_guid = versions.guid and services.version_number = $ServiceVersionNumber
     join applications on applications.deleted_at is null and applications.guid = versions.application_guid
     join organizations on organizations.deleted_at is null and organizations.guid = applications.organization_guid
    where versions.deleted_at is null
  """

  private val InsertQuery = """
    insert into versions
    (guid, application_guid, version, version_sort_key, original, created_by_guid)
    values
    ({guid}::uuid, {application_guid}::uuid, {version}, {version_sort_key}, {original}, {created_by_guid}::uuid)
  """

  private val InsertServiceQuery = """
    insert into cache.services
    (guid, version_guid, version_number, json, created_by_guid)
    values
    ({guid}::uuid, {version_guid}::uuid, {version_number}, {json}::json, {user_guid}::uuid)
  """

  private val SoftDeleteServiceByVersionGuidAndVersionNumberQuery = """
    update cache.services
       set deleted_at = now(),
           deleted_by_guid = {user_guid}::uuid
     where deleted_at is null
       and version_guid = {version_guid}::uuid
       and version_number = {version_number}
  """

  def create(user: User, application: Application, version: String, original: JsObject, service: Service): Version = {
    assert(
      !(original \ "name").asOpt[String].isEmpty,
      "original is missing name"
    )

    val guid = UUID.randomUUID

    DB.withTransaction { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'application_guid -> application.guid,
        'original -> original.toString,
        'version -> version.trim,
        'version_sort_key -> VersionTag(version.trim).sortKey,
        'created_by_guid -> user.guid
      ).execute()

      softDeleteService(c, user, guid)
      insertService(c, user, guid, service)
    }

    global.Actors.mainActor ! actors.MainActor.Messages.VersionCreated(guid)

    findAll(Authorization.All, guid = Some(guid), limit = 1).headOption.getOrElse {
      sys.error("Failed to create version")
    }
  }

  def softDelete(deletedBy: User, version: Version) {
    SoftDelete.delete("versions", deletedBy, version.guid)
  }

  def replace(user: User, version: Version, application: Application, original: JsObject, service: Service): Version = {
    DB.withTransaction { implicit c =>
      softDelete(user, version)
      VersionsDao.create(user, application, version.version, original, service)
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

  def findByGuid(authorization: Authorization, guid: UUID): Option[Version] = {
    findAll(authorization, guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    authorization: Authorization,
    applicationGuid: Option[UUID] = None,
    guid: Option[UUID] = None,
    version: Option[String] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Version] = {
    val sql = Seq(
      Some(BaseQuery.trim + " and services.guid is not null "),
      authorization.applicationFilter().map(v => "and " + v),
      guid.map { v => "and versions.guid = {guid}::uuid" },
      applicationGuid.map { _ => "and versions.application_guid = {application_guid}::uuid" },
      version.map { v => "and versions.version = {version}" },
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
        val original = row[Option[String]]("original").getOrElse {
          row[String]("old_json")
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
          original = original.toString,
          service = Json.toJson(service).as[JsObject]
        )
      }.toSeq
    }
  }

  def migrate(): Map[String, Int] = {
    var user: Option[User] = None
    val sql = BaseQuery.trim + " and services.guid is null and versions.original != '{}' "

    var good = 0
    var bad = 0

    DB.withConnection { implicit c =>
      val versions = SQL(sql)().toList
      versions.map { row =>
        val versionGuid = row[UUID]("guid")
        Logger.info(s"Migrating version[${versionGuid}]")

        val config = ServiceConfiguration(
          orgNamespace = row[String]("organization_namespace"),
          version = row[String]("version")
        )

        val original = row[Option[String]]("original").getOrElse {
          row[String]("old_json")
        }

        try {
          val service = core.ServiceValidator(config, original).service.get
          if (user.isEmpty) {
            user = Some(UsersDao.findByEmail(UsersDao.AdminUserEmail).getOrElse {
              sys.error(s"Failed to create background user w/ email[${UsersDao.AdminUserEmail}]")
            })
          }

          insertService(c, user.get, versionGuid, service)
          good += 1
        } catch {
          case e: Throwable => {
            Logger.error(s"Error migrating version[${versionGuid}] to service versionNumber[$ServiceVersionNumber]: $e")
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
      'version_number -> ServiceVersionNumber,
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
      'version_number -> ServiceVersionNumber,
      'json -> Json.toJson(service).as[JsObject].toString.trim,
      'user_guid -> user.guid
    ).execute()
  }

}
