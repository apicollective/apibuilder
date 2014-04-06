package db

import lib.{ Constants, UrlKey }
import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

case class ServiceDao(guid: UUID, org: Organization, name: String, key: String, description: Option[String]) {

  def softDelete(deletedBy: User) {
    DB.withConnection { implicit c =>
      SQL("""
          update services set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now() where guid = {guid}::uuid and deleted_at is null
          """).on('deleted_by_guid -> deletedBy.guid, 'guid -> guid).execute()
    }
  }

}

case class ServiceQuery(guid: Option[UUID] = None,
                        org: Option[Organization] = None,
                        name: Option[String] = None,
                        key: Option[String] = None,
                        limit: Int = 50,
                        offset: Int = 0)

object ServiceDao {

  private val BaseQuery = """
    select services.guid::varchar, services.name, services.key, services.description,
           organizations.guid::varchar as organization_guid,
           organizations.name as organization_name,
           organizations.key as organization_key
      from services
      join organizations on organizations.guid = services.organization_guid
     where services.deleted_at is null
  """

  def upsert(createdBy: User, org: Organization, name: String): ServiceDao = {
    val key = UrlKey.generate(name)

    findByOrganizationAndKey(org, key) match {
      case Some(s: ServiceDao) => {
        if (s.name == name && s.key == key && s.org.guid == org.guid) {
          s
        } else {
          DB.withTransaction { implicit c =>
            s.softDelete(createdBy)
            create(createdBy, org, name)
          }
        }
      }
      case None => {
        create(createdBy, org, name)
      }
    }
  }

  def update(updatedBy: User, dao: ServiceDao) {
    DB.withConnection { implicit c =>
      SQL("""
          update services
             set name = {name},
                 description = {description},
                 updated_by_guid = {updated_by_guid}
           where guid = {guid}
          """).on('guid -> dao.guid,
                  'name -> dao.name,
                  'description -> dao.description,
                  'updated_by_guid -> updatedBy.guid).execute()
    }
  }

  private def create(createdBy: User, org: Organization, name: String): ServiceDao = {
    val guid = UUID.randomUUID
    val key = UrlKey.generate(name)
    DB.withConnection { implicit c =>
      SQL("""
          insert into services
          (guid, organization_guid, name, key, created_by_guid, updated_by_guid)
          values
          ({guid}::uuid, {organization_guid}, {name}, {key}, {created_by_guid}::uuid, {created_by_guid}::uuid)
          """).on('guid -> guid,
                  'organization_guid -> org.guid,
                  'name -> name,
                  'key -> key,
                  'created_by_guid -> createdBy.guid,
                  'updated_by_guid -> createdBy.guid).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create service")
    }
  }

  def findByOrganizationAndName(org: Organization, name: String): Option[ServiceDao] = {
    findByOrganizationAndKey(org, UrlKey.generate(name))
  }

  def findByOrganizationAndKey(org: Organization, key: String): Option[ServiceDao] = {
    findAll(ServiceQuery(org = Some(org), key = Some(key), limit = 1)).headOption
  }

  def findByGuid(guid: UUID): Option[ServiceDao] = {
    findAll(ServiceQuery(guid = Some(guid),  limit = 1)).headOption
  }

  def findAll(query: ServiceQuery): Seq[ServiceDao] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      query.guid.map { v => "and services.guid = {guid}::uuid" },
      query.org.map { org => "and services.organization_guid = {organization_guid}::uuid" },
      query.name.map { v => "and services.name = {name}" },
      query.key.map { v => "and services.key = lower(trim({key}))" },
      Some(s"order by lower(services.name) limit ${query.limit} offset ${query.offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq(
      query.guid.map { v => 'guid -> toParameterValue(v) },
      query.org.map { org => 'organization_guid -> toParameterValue(org.guid) },
      query.name.map { v => 'name -> toParameterValue(v) },
      query.key.map { v => 'key -> toParameterValue(v) }
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        ServiceDao(guid = UUID.fromString(row[String]("guid")),
                   name = row[String]("name"),
                   key = row[String]("key"),
                   description = row[Option[String]]("description"),
                   org = Organization(guid = UUID.fromString(row[String]("organization_guid")),
                                      name = row[String]("organization_name"),
                                      key = row[String]("organization_key")))
        }.toSeq
    }
  }

}
