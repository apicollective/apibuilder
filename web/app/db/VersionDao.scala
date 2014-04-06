package db

import lib.{ Constants, VersionSortKey }
import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

case class VersionDao(guid: UUID, serviceGuid: UUID, version: String, json: String)

object VersionDao {

  private val BaseQuery = """
    select guid::varchar, service_guid::varchar, version, json::varchar
     from versions
    where deleted_at is null
  """

  def upsert(serviceDao: ServiceDao, version: String, jsonInput: String): VersionDao = {
    val json = jsonInput.trim
    val existing = findByServiceAndVersion(serviceDao, version)
    if (existing.isEmpty) {
      create(serviceDao, version, json)
    } else if (existing.get == VersionDao(existing.get.guid, serviceDao.guid, version, json)) {
      existing.get
    } else {
      DB.withTransaction { implicit c =>
        softDelete(existing.get)
        create(serviceDao, version, json)
      }
    }
  }

  private def create(serviceDao: ServiceDao, version: String, json: String): VersionDao = {
    val guid = UUID.randomUUID

    DB.withConnection { implicit c =>
      SQL("""
          insert into versions
          (guid, service_guid, version, version_sort_key, json, created_by_guid)
          values
          ({guid}::uuid, {service_guid}::uuid, {version}, {version_sort_key}, {json}::json, {created_by_guid}::uuid)
          """).on('guid -> guid,
                  'service_guid -> serviceDao.guid,
                  'version -> version,
                  'version_sort_key -> VersionSortKey.generate(version),
                  'json -> json,
                  'created_by_guid -> Constants.DefaultUserGuid).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create version")
    }
  }

  def softDelete(versionDao: VersionDao) {
    DB.withConnection { implicit c =>
      SQL("""
          update versions set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now() where guid = {guid}::uuid
          """).on('deleted_by_guid -> Constants.DefaultUserGuid, 'guid -> versionDao.guid).execute()
    }
  }

  def latestVersionForService(serviceDao: ServiceDao): Option[VersionDao] = {
    findAllByService(serviceDao).headOption
  }

  def findAllByService(service: ServiceDao): Seq[VersionDao] = {
    DB.withConnection { implicit c =>
      SQL(BaseQuery + " and service_guid = {service_guid}::uuid order by version_sort_key desc, created_at desc")
                       .on('service_guid -> service.guid)().toList.map { row  =>mapRow(row) }.toSeq
    }
  }

  def findByServiceAndVersion(service: ServiceDao, version: String): Option[VersionDao] = {
    DB.withConnection { implicit c =>
      SQL(BaseQuery + " and service_guid = {service_guid}::uuid and version = {version}").on('service_guid -> service.guid, 'version -> version)().map { row =>
        mapRow(row)
      }.headOption
    }
  }

  def findByGuid(guid: UUID): Option[VersionDao] = {
    DB.withConnection { implicit c =>
      SQL(BaseQuery + " and guid = {guid}::uuid").on('guid -> guid)().map { row =>
        mapRow(row)
      }.toSeq.headOption
    }
  }

  private def mapRow(row: anorm.SqlRow): VersionDao = {
    VersionDao(guid = UUID.fromString(row[String]("guid")),
                 serviceGuid = UUID.fromString(row[String]("service_guid")),
                 version = row[String]("version"),
                 json = row[String]("json"))

  }

}
