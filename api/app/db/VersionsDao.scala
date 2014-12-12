package db

import com.gilt.apidoc.models.{Service, User, Version, Visibility}
import core.VersionTag
import anorm._
import play.api.db._
import play.api.libs.json._
import play.api.Play.current
import java.util.UUID

case class VersionForm(
  json: String,
  visibility: Option[Visibility] = None
)

object VersionForm {
  import com.gilt.apidoc.models.json._
  implicit val versionFormReads = Json.reads[VersionForm]
}

object VersionsDao {

  private val LatestVersion = "latest"

  private val BaseQuery = """
    select guid, version, json::varchar
     from versions
    where deleted_at is null
  """

  def create(user: User, service: Service, version: String, json: String): Version = {
    val v = Version(guid = UUID.randomUUID,
                    version = version,
                    json = json)

    DB.withConnection { implicit c =>
      SQL("""
          insert into versions
          (guid, service_guid, version, version_sort_key, json, created_by_guid)
          values
          ({guid}::uuid, {service_guid}::uuid, {version}, {version_sort_key}, {json}::json, {created_by_guid}::uuid)
          """).on('guid -> v.guid,
                  'service_guid -> service.guid,
                  'version -> v.version,
                  'version_sort_key -> VersionTag(v.version).sortKey,
                  'json -> v.json,
                  'created_by_guid -> user.guid).execute()
    }

    v
  }

  def softDelete(deletedBy: User, version: Version) {
    SoftDelete.delete("versions", deletedBy, version.guid)
  }

  def replace(user: User, version: Version, service: Service, newJson: String): Version = {
    DB.withTransaction { implicit c =>
      softDelete(user, version)
      VersionsDao.create(user, service, version.version, newJson)
    }
  }

  def findVersion(authorization: Authorization, orgKey: String, serviceKey: String, version: String): Option[Version] = {
    ServicesDao.findByOrganizationKeyAndServiceKey(authorization, orgKey, serviceKey).flatMap { service =>
      if (version == LatestVersion) {
        VersionsDao.findAll(service_guid = Some(service.guid), limit = 1).headOption
      } else {
        VersionsDao.findByServiceAndVersion(service, version)
      }
    }
  }

  def findByServiceAndVersion(service: Service, version: String): Option[Version] = {
    VersionsDao.findAll(
      service_guid = Some(service.guid),
      version = Some(version),
      limit = 1
    ).headOption
  }

  def findAll(
    service_guid: Option[UUID] = None,
    guid: Option[String] = None,
    version: Option[String] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Version] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and versions.guid = {guid}::uuid" },
      service_guid.map { _ => "and versions.service_guid = {service_guid}::uuid" },
      version.map { v => "and versions.version = {version}" },
      Some(s"order by versions.version_sort_key desc, versions.created_at desc limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _),
      service_guid.map('service_guid -> _.toString),
      version.map('version ->_)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        Version(
          guid = row[UUID]("guid"),
          version = row[String]("version"),
          json = row[String]("json")
        )
      }.toSeq
    }
  }

}
