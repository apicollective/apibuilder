package db

import lib.VersionSortKey
import anorm._
import play.api.db._
import play.api.libs.json._
import play.api.Play.current
import java.util.UUID

case class Version(guid: String, version: String, json: String)

object Version {

  implicit val versionWrites = Json.writes[Version]

}

case class VersionForm(json: String)

object VersionForm {

  implicit val versionFormReads = Json.reads[VersionForm]

}

object VersionDao {

  implicit val versionReads = Json.reads[Version]
  implicit val versionWrites = Json.writes[Version]

  private val BaseQuery = """
    select guid::varchar, version, json::varchar
     from versions
    where deleted_at is null
  """

  def create(user: User, service: Service, version: String, json: String): Version = {
    val v = Version(guid = UUID.randomUUID.toString,
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
                  'version_sort_key -> VersionSortKey.generate(v.version),
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
      VersionDao.create(user, service, version.version, newJson)
    }
  }

  def findVersion(user: User, orgKey: String, serviceKey: String, version: String): Option[Version] = {
    OrganizationDao.findByUserAndKey(user, orgKey).flatMap { org =>
      ServiceDao.findByOrganizationAndKey(org, serviceKey).flatMap { service =>
        if (version == "latest") {
          VersionDao.findAll(service_guid = Some(service.guid), limit = 1).headOption
        } else {
          VersionDao.findByServiceAndVersion(service, version)
        }
      }
    }
  }

  def findByServiceAndVersion(service: Service, version: String): Option[Version] = {
    VersionDao.findAll(service_guid = Some(service.guid),
                       version = Some(version),
                       limit = 1).headOption
  }

  def findAll(service_guid: Option[String] = None,
              guid: Option[String] = None,
              version: Option[String] = None,
              limit: Int = 50,
              offset: Int = 0): Seq[Version] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and versions.guid = {guid}::uuid" },
      service_guid.map { _ => "and versions.service_guid = {service_guid}::uuid" },
      version.map { v => "and versions.version = {version}" },
      Some(s"order by versions.version_sort_key desc, versions.created_at desc limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _),
      service_guid.map('service_guid -> _),
      version.map('version ->_)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        Version(guid = row[String]("guid"),
                version = row[String]("version"),
                json = row[String]("json"))
        }.toSeq
    }
  }

}
