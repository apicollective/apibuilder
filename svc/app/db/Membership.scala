package db

import lib.Constants
import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

case class Membership(guid: UUID, org: Organization, user: User, role: String) {

  /**
   * Removes this membership record and logs the action
   */
  def remove(user: User) {
    val message = "Removed user[%s][%s] as %s".format(user.guid, user.email, role)
    DB.withTransaction { implicit conn =>
      OrganizationLog.create(user, org, message)
      Membership.softDelete(user, guid)
    }
  }

}

case class MembershipQuery(guid: Option[UUID] = None,
                                       org: Option[Organization] = None,
                                       user: Option[User] = None,
                                       role: Option[String] = None,
                                       limit: Int = 50,
                                       offset: Int = 0)

object Membership {

  private val BaseQuery = """
    select memberships.guid::varchar,
           role,
           organizations.guid::varchar as organization_guid,
           organizations.name as organization_name,
           organizations.key as organization_key,
           users.guid::varchar as user_guid,
           users.email as user_email,
           users.name as user_name,
           users.image_url as user_image_url
      from memberships
      join organizations on organizations.guid = memberships.organization_guid
      join users on users.guid = memberships.user_guid
     where memberships.deleted_at is null
  """

  def upsert(createdBy: User, organization: Organization, user: User, role: String): Membership = {
    findByOrganizationAndUserAndRole(organization, user, role) match {
      case Some(r: Membership) => r
      case None => {
        create(createdBy, organization, user, role)
      }
    }
  }

  private def create(createdBy: User, organization: Organization, user: User, role: String): Membership = {
    val guid = UUID.randomUUID
    DB.withConnection { implicit c =>
      SQL("""
          insert into memberships
          (guid, organization_guid, user_guid, role, created_by_guid)
          values
          ({guid}::uuid, {organization_guid}::uuid, {user_guid}::uuid, {role}, {created_by_guid}::uuid)
          """).on('guid -> guid,
                  'organization_guid -> organization.guid,
                  'user_guid -> user.guid,
                  'role -> role,
                  'created_by_guid -> createdBy.guid).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create membership")
    }
  }

  def softDelete(user: User, guid: UUID) {
    DB.withConnection { implicit c =>
      SQL("""
          update memberships
             set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now()
           where memberships.guid = {guid}::uuid
             and deleted_at is null
          """).on('deleted_by_guid -> user.guid, 'guid -> guid).execute()
    }
  }

  private def findByGuid(guid: UUID): Option[Membership] = {
    findAll(MembershipQuery(guid = Some(guid))).headOption
  }

  def findAllForOrganization(org: Organization): Seq[Membership] = {
    findAll(MembershipQuery(org = Some(org)))
  }

  def findAllForOrganizationAndUser(org: Organization, user: User): Seq[Membership] = {
    findAll(MembershipQuery(org = Some(org), user = Some(user)))
  }

  def findByOrganizationAndUserAndRole(org: Organization, user: User, role: String): Option[Membership] = {
    findAll(MembershipQuery(org = Some(org), user = Some(user), role = Some(role), limit = 1)).headOption
  }

  def findAll(query: MembershipQuery): Seq[Membership] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      query.guid.map { v => "and memberships.guid = {guid}::uuid" },
      query.org.map { v => "and memberships.organization_guid = {organization_guid}::uuid" },
      query.user.map { v => "and memberships.user_guid = {user_guid}::uuid" },
      query.role.map { v => "and role = {role}" },
      Some(s"order by lower(users.name), lower(users.email) limit ${query.limit} offset ${query.offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq(
      query.guid.map { v => 'guid -> toParameterValue(v) },
      query.org.map { o => 'organization_guid -> toParameterValue(o.guid) },
      query.user.map { u => 'user_guid -> toParameterValue(u.guid) },
      query.role.map { v => 'role -> toParameterValue(v) }
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        Membership(guid = UUID.fromString(row[String]("guid")),
                               org = Organization(guid = UUID.fromString(row[String]("organization_guid")),
                                                  name = row[String]("organization_name"),
                                                  key = row[String]("organization_key")),
                               user = User(guid = UUID.fromString(row[String]("user_guid")),
                                           email = row[String]("user_email"),
                                           name = row[Option[String]]("user_name"),
                                           imageUrl = row[Option[String]]("user_image_url")),
                               role = row[String]("role"))
      }.toSeq
    }
  }

}
