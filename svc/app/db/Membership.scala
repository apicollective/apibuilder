package db

import core.Role
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

case class Membership(guid: String, org: Organization, user: User, role: String)

object Membership {

  implicit val membershipWrites = Json.writes[Membership]

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

  def upsert(createdBy: User, organization: Organization, user: User, role: Role): Membership = {
    findByOrganizationAndUserAndRole(organization, user, role.key) match {
      case Some(r: Membership) => r
      case None => {
        create(createdBy, organization, user, role)
      }
    }
  }

  private def create(createdBy: User, organization: Organization, user: User, role: Role): Membership = {
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

  def findByOrganizationAndUserAndRole(organization: Organization, user: User, role: String): Option[Membership] = {
    findAll(organization_guid = Some(organization.guid), user_guid = Some(user.guid), role = Some(role)).headOption
  }

  def findAll(user: Option[User] = None,
              guid: Option[String] = None,
              organization_guid: Option[String] = None,
              organization_key: Option[String] = None,
              user_guid: Option[String] = None,
              role: Option[String] = None,
              limit: Int = 50,
              offset: Int = 0): Seq[Membership] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      user.map { u => """
                and organization_guid in (select organization_guid
                                            from memberships
                                           where deleted_at is null
                                             and user_guid = {authorized_user_guid}::uuid)
                """ },
      guid.map { v => "and memberships.guid = {guid}::uuid" },
      organization_guid.map { v => "and memberships.organization_guid = {organization_guid}::uuid" },
      organization_key.map { v => "and memberships.organization_guid = (select guid from organizations where deleted_at is null and key = {organization_key})" },
      user_guid.map { v => "and memberships.user_guid = {user_guid}::uuid" },
      role.map { v => "and role = {role}" },
      Some(s"order by lower(users.name), lower(users.email) limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq(
      user.map { u => 'authorized_user_guid -> toParameterValue(u.guid) },
      guid.map { v => 'guid -> toParameterValue(v) },
      organization_guid.map { v => 'organization_guid -> toParameterValue(v) },
      organization_key.map { v => 'organization_key -> toParameterValue(v) },
      user_guid.map { v => 'user_guid -> toParameterValue(v) },
      role.map { v => 'role -> toParameterValue(v) }
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        Membership(guid = row[String]("guid"),
                   org = Organization(guid = row[String]("organization_guid"),
                                      name = row[String]("organization_name"),
                                      key = row[String]("organization_key")),
                   user = User(guid = row[String]("user_guid"),
                               email = row[String]("user_email"),
                               name = row[Option[String]]("user_name"),
                               imageUrl = row[Option[String]]("user_image_url")),
                   role = row[String]("role"))
      }.toSeq
    }
  }

}
