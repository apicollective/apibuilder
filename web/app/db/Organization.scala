package db

import lib.UrlKey
import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

case class Organization(guid: UUID, name: String, key: String)

case class OrganizationQuery(guid: Option[UUID] = None,
                             user: Option[User] = None,
                             key: Option[String] = None,
                             limit: Int = 50,
                             offset: Int = 0)

object Organization {

  private val BaseQuery = """
    select guid::varchar, name, key
      from organizations
     where deleted_at is null
  """

  /**
   * Creates the org and assigns the user as its administrator.
   */
  def createWithAdministrator(user: User, name: String): Organization = {
    DB.withTransaction { implicit c =>
      val org = create(user, name)
      Membership.upsert(user, org, user, "admin")
      MembershipLog.create(user, org, "Created organization and joined as administrator")
      org
    }
  }

  def create(createdBy: User, name: String): Organization = {
    val guid = UUID.randomUUID
    val key = UrlKey.generate(name)
    DB.withConnection { implicit c =>
      SQL("""
          insert into organizations
          (guid, name, key, created_by_guid)
          values
          ({guid}::uuid, {name}, {key}, {created_by_guid}::uuid)
          """).on('guid -> guid,
                  'name -> name,
                  'key -> key,
                  'created_by_guid -> createdBy.guid).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create organization")
    }
  }

  def findByName(name: String): Option[Organization] = {
    findByKey(UrlKey.generate(name))
  }

  def findByKey(key: String): Option[Organization] = {
    findAll(OrganizationQuery(key = Some(key), limit = 1)).headOption
  }

  def findByGuid(guid: UUID): Option[Organization] = {
    findAll(OrganizationQuery(guid = Some(guid), limit = 1)).headOption
  }

  def findAll(query: OrganizationQuery): Seq[Organization] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      query.guid.map { v => "and guid = {guid}::uuid" },
      query.user.map { u => "and guid in (select organization_guid from memberships where deleted_at is null and user_guid = {user_guid}::uuid)" },
      query.key.map { v => "and key = lower(trim({key}))" },
      Some(s"order by lower(name) limit ${query.limit} offset ${query.offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq(
      query.guid.map { v => 'guid -> toParameterValue(v) },
      query.user.map { u => 'user_guid -> toParameterValue(u.guid) },
      query.key.map { v => 'key -> toParameterValue(v) }
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        Organization(guid = UUID.fromString(row[String]("guid")),
                     name = row[String]("name"),
                     key = row[String]("key"))
      }.toSeq
    }
  }

}
