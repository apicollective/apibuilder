package db

import lib.{ Constants, UrlKey }
import anorm._
import play.api.db._
import play.api.libs.json._
import play.api.Play.current
import java.util.UUID

case class Service(guid: String, name: String, key: String, description: Option[String]) {

  def softDelete(deletedBy: User) {
    DB.withConnection { implicit c =>
      SQL("""
          update services set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now() where guid = {guid}::uuid and deleted_at is null
          """).on('deleted_by_guid -> deletedBy.guid, 'guid -> guid).execute()
    }
  }

}

case class ServiceQuery(orgKey: String,
                        guid: Option[UUID] = None,
                        name: Option[String] = None,
                        key: Option[String] = None,
                        limit: Int = 50,
                        offset: Int = 0)

object Service {

  implicit val serviceReads = Json.reads[Service]
  implicit val serviceWrites = Json.writes[Service]

  private val BaseQuery = """
    select services.guid::varchar, services.name, services.key, services.description,
           organizations.guid::varchar as organization_guid,
           organizations.name as organization_name,
           organizations.key as organization_key
      from services
      join organizations on organizations.guid = services.organization_guid
     where services.deleted_at is null
  """

  def update(updatedBy: User, dao: Service) {
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

  def create(createdBy: User, org: Organization, name: String, keyOption: Option[String] = None): Service = {
    val guid = UUID.randomUUID
    val key = keyOption.getOrElse(UrlKey.generate(name))
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

    findByOrganizationAndKey(org, key).getOrElse {
      sys.error("Failed to create service")
    }
  }

  def findByOrganizationAndName(org: Organization, name: String): Option[Service] = {
    val key = UrlKey.generate(name)
    findByOrganizationAndKey(org, key)
  }

  def findByOrganizationAndKey(org: Organization, key: String): Option[Service] = {
    findAll(ServiceQuery(orgKey = org.key, key = Some(key), limit = 1)).headOption
  }

  def findAll(query: ServiceQuery): Seq[Service] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      query.guid.map { v => "and services.guid = {guid}::uuid" },
      Some("and services.organization_guid = (select guid from organizations where deleted_at is null and key = {organization_key})"),
      query.name.map { v => "and services.name = {name}" },
      query.key.map { v => "and services.key = lower(trim({key}))" },
      Some(s"order by lower(services.name) limit ${query.limit} offset ${query.offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq(
      query.guid.map { v => 'guid -> toParameterValue(v) },
      Some('organization_key -> toParameterValue(query.orgKey)),
      query.name.map { v => 'name -> toParameterValue(v) },
      query.key.map { v => 'key -> toParameterValue(v) }
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        Service(guid = row[String]("guid"),
                   name = row[String]("name"),
                   key = row[String]("key"),
                   description = row[Option[String]]("description"))
        }.toSeq
    }
  }

}
