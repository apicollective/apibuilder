package db

import com.gilt.apidoc.models.{Application, User, Version, VersionForm, Visibility}
import lib.VersionTag
import anorm._
import play.api.db._
import play.api.libs.json._
import play.api.Play.current
import java.util.UUID

object VersionsDao {

  private val LatestVersion = "latest"

  private val BaseQuery = """
    select versions.guid, versions.version, versions.json::varchar
     from versions
     join applications on applications.deleted_at is null and applications.guid = versions.application_guid
    where versions.deleted_at is null
  """

  private val InsertQuery = """
    insert into versions
    (guid, application_guid, version, version_sort_key, json, created_by_guid)
    values
    ({guid}::uuid, {application_guid}::uuid, {version}, {version_sort_key}, {json}::json, {created_by_guid}::uuid)
  """

  def create(user: User, application: Application, version: String, json: JsObject): Version = {
    val guid = UUID.randomUUID

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'application_guid -> application.guid,
        'version -> version.trim,
        'version_sort_key -> VersionTag(version.trim).sortKey,
        'json -> json.toString.trim,
        'created_by_guid -> user.guid
      ).execute()
    }

    global.Actors.mainActor ! actors.MainActor.Messages.VersionCreated(guid)

    findAll(Authorization.All, guid = Some(guid), limit = 1).headOption.getOrElse {
      sys.error("Failed to create version")
    }
  }

  def softDelete(deletedBy: User, version: Version) {
    SoftDelete.delete("versions", deletedBy, version.guid)
  }

  def replace(user: User, version: Version, application: Application, newJson: JsObject): Version = {
    DB.withTransaction { implicit c =>
      softDelete(user, version)
      VersionsDao.create(user, application, version.version, newJson)
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
      Some(BaseQuery.trim),
      authorization.applicationFilter("applications").map(v => "and " + v),
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
        Version(
          guid = row[UUID]("guid"),
          version = row[String]("version"),
          json = Json.parse(row[String]("json")).as[JsObject]
        )
      }.toSeq
    }
  }

}
