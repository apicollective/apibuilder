package db

import lib.VersionSortKey
import anorm._
import play.api.db._
import play.api.libs.json._
import play.api.Play.current
import java.util.UUID

case class Version(guid: String, version: String)

object Version {

  implicit val versionWrites = Json.writes[Version]

}

case class DetailedVersion(guid: String, version: String, json: String)

object DetailedVersion {

  implicit val versionWrites = Json.writes[DetailedVersion]

}

object VersionDao {

  implicit val versionReads = Json.reads[Version]
  implicit val versionWrites = Json.writes[Version]

  private val BaseQuery = """
    select guid::varchar, version
     from versions
    where deleted_at is null
  """

  private val DetailedBaseQuery = """
    select guid::varchar, version, json::varchar
      from versions
     where guid = {guid}::uuid
  """

  def create(user: User, service: Service, version: String, json: String): DetailedVersion = {
    val v = DetailedVersion(guid = UUID.randomUUID.toString,
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
    DB.withConnection { implicit c =>
      SQL("""
          update versions set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now() where guid = {guid}::uuid and deleted_at is null
          """).on('deleted_by_guid -> deletedBy.guid, 'guid -> version.guid).execute()
    }
  }

  def replace(user: User, version: Version, service: Service, newJson: String): DetailedVersion = {
    DB.withTransaction { implicit c =>
      softDelete(user, version)
      VersionDao.create(user, service, version.version, newJson)
    }
  }

  def findByServiceAndVersion(service: Service, version: String): Option[Version] = {
    VersionDao.findAll(service_guid = service.guid,
                       version = Some(version),
                       limit = 1).headOption
  }

  def getDetails(version: Version): Option[DetailedVersion] = {
    DB.withConnection { implicit c =>
      SQL(DetailedBaseQuery).on('guid -> version.guid)().toList.map { row =>
        DetailedVersion(guid = row[String]("guid"),
                        version = row[String]("version"),
                        json = row[String]("json"))
        }.toSeq.headOption
    }
  }

  def findAll(service_guid: String,
              guid: Option[String] = None,
              version: Option[String] = None,
              limit: Int = 50,
              offset: Int = 0): Seq[Version] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and versions.guid = {guid}::uuid" },
      Some("and versions.service_guid = {service_guid}::uuid"),
      version.map { v => "and versions.version = {version}" },
      Some(s"order by versions.version_sort_key desc, versions.created_at desc limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _),
      Some('service_guid -> service_guid),
      version.map('version ->_)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        Version(guid = row[String]("guid"),
                version = row[String]("version"))
        }.toSeq
    }
  }

}
