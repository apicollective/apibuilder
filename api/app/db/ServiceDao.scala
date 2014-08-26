package db

import core.UrlKey
import anorm._
import play.api.db._
import play.api.libs.json._
import play.api.Play.current
import java.util.UUID

case class Service(
  guid: String,
  name: String,
  key: String,
  visibility: String,
  description: Option[String]
)

object Service {

  implicit val serviceWrites = Json.writes[Service]

}

object ServiceDao {

  private val BaseQuery = """
    select services.guid::varchar, services.name, services.key, services.description,
           coalesce(services.visibility, 'organization') as visibility,
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
          ({guid}::uuid, {organization_guid}::uuid, {name}, {key}, {created_by_guid}::uuid, {created_by_guid}::uuid)
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

  def softDelete(deletedBy: User, service: Service) {
    SoftDelete.delete("services", deletedBy, service.guid)
  }

  def findByOrganizationAndName(org: Organization, name: String): Option[Service] = {
    val key = UrlKey.generate(name)
    findByOrganizationAndKey(org, key)
  }

  def findByOrganizationAndKey(org: Organization, key: String): Option[Service] = {
    findAll(orgKey = org.key, key = Some(key), limit = 1).headOption
  }

  def findAll(orgKey: String,
              guid: Option[String] = None,
              name: Option[String] = None,
              key: Option[String] = None,
              limit: Int = 50,
              offset: Int = 0): Seq[Service] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and services.guid = {guid}::uuid" },
      Some("and services.organization_guid = (select guid from organizations where deleted_at is null and key = {organization_key})"),
      name.map { v => "and services.name = {name}" },
      key.map { v => "and services.key = lower(trim({key}))" },
      Some(s"order by lower(services.name) limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _),
      Some('organization_key -> orgKey),
      name.map('name -> _),
      key.map('key ->_)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        Service(
          guid = row[String]("guid"),
          name = row[String]("name"),
          key = row[String]("key"),
          visibility = row[String]("visibility"),
          description = row[Option[String]]("description")
        )
        }.toSeq
    }
  }

}
