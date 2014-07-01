package db

import core.Role
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

case class Membership(guid: String, organization: Organization, user: User, role: String)

object Membership {

  implicit val membershipWrites = Json.writes[Membership]

  private val InsertQuery = """
    insert into memberships
    (guid, organization_guid, user_guid, role, created_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {user_guid}::uuid, {role}, {created_by_guid}::uuid)
  """

  private val BaseQuery = """
    select memberships.guid::varchar,
           role,
           organizations.guid::varchar as organization_guid,
           organizations.name as organization_name,
           organizations.key as organization_key,
           users.guid::varchar as user_guid,
           users.email as user_email,
           users.name as user_name
      from memberships
      join organizations on organizations.guid = memberships.organization_guid
      join users on users.guid = memberships.user_guid
     where memberships.deleted_at is null
  """

  def upsert(createdBy: User, organization: Organization, user: User, role: Role): Membership = {
    val membership = findByOrganizationAndUserAndRole(organization, user, role) match {
      case Some(r: Membership) => r
      case None => {
        create(createdBy, organization, user, role)
      }
    }

    // If we made this user an admin, and s/he already exists as a
    // member, remove the member role - this is akin to an upgrade
    // in membership from member to admin.
    if (role == Role.Admin) {
      findByOrganizationAndUserAndRole(organization, user: User, Role.Member).foreach { membership =>
        softDelete(user, membership: Membership)
      }
    }

    membership
  }

  private def create(createdBy: User, organization: Organization, user: User, role: Role): Membership = {
    val guid = UUID.randomUUID
    DB.withConnection { implicit c =>
      SQL(InsertQuery).on('guid -> guid,
                          'organization_guid -> organization.guid,
                          'user_guid -> user.guid,
                          'role -> role.key,
                          'created_by_guid -> createdBy.guid).execute()
    }

    findAll(guid = Some(guid.toString), limit = 1).headOption.getOrElse {
      sys.error("Failed to create membership")
    }
  }

  def softDelete(user: User, membership: Membership) {
    SoftDelete.delete("memberships", user, membership.guid)
  }

  def isUserAdmin(user: User, organization: Organization): Boolean = {
    findByOrganizationAndUserAndRole(organization, user, Role.Admin) match {
      case None => false
      case Some(m: Membership) => true
    }
  }

  def findByOrganizationAndUserAndRole(organization: Organization, user: User, role: Role): Option[Membership] = {
    findAll(organization_guid = Some(organization.guid), user_guid = Some(user.guid), role = Some(role.key)).headOption
  }

  def findAll(guid: Option[String] = None,
              organization_guid: Option[String] = None,
              organization_key: Option[String] = None,
              user_guid: Option[String] = None,
              role: Option[String] = None,
              limit: Int = 50,
              offset: Int = 0): Seq[Membership] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and memberships.guid = {guid}::uuid" },
      organization_guid.map { v => "and memberships.organization_guid = {organization_guid}::uuid" },
      organization_key.map { v => "and memberships.organization_guid = (select guid from organizations where deleted_at is null and key = {organization_key})" },
      user_guid.map { v => "and memberships.user_guid = {user_guid}::uuid" },
      role.map { v => "and role = {role}" },
      Some(s"order by lower(users.name), lower(users.email) limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _ ),
      organization_guid.map('organization_guid -> _ ),
      organization_key.map('organization_key -> _ ),
      user_guid.map('user_guid -> _),
      role.map('role -> _)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        Membership(guid = row[String]("guid"),
                   organization = Organization(guid = row[String]("organization_guid"),
                                               name = row[String]("organization_name"),
                                               key = row[String]("organization_key")),
                   user = User(guid = row[String]("user_guid"),
                               email = row[String]("user_email"),
                               name = row[Option[String]]("user_name")),
                   role = row[String]("role"))
      }.toSeq
    }
  }

}
