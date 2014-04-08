package db

import anorm._
import play.api.db._
import play.api.Play.current
import java.sql.Timestamp
import play.api.libs.json._
import java.util.UUID

case class MembershipRequestJson(guid: String,
                                 created_at: String, // TODO Timestamp type
                                 organization_guid: String,
                                 organization_name: String,
                                 user_guid: String,
                                 user_email: String,
                                 user_name: Option[String],
                                 role: String)

object MembershipRequestJson {
  implicit val membershipRequestJsonWrites = Json.writes[MembershipRequestJson]
}


case class MembershipRequest(guid: String,
                             created_at: String, // TODO Timestamp type
                             org: Organization,
                             user: User,
                             role: String) {

  lazy val json = MembershipRequestJson(guid = guid.toString,
                                        created_at = created_at,
                                        organization_guid = org.guid,
                                        organization_name = org.name,
                                        user_guid = user.guid.toString,
                                        user_email = user.guid,
                                        user_name = user.name,
                                        role = role)

  /**
   * Approves this request. The request will be deleted, the
   * action logged, and the * user added to the organization
   * in this role if not already in this role for this org.
   */
  def approve(createdBy: User) {
    val message = "Approved membership request for %s to join as %s".format(user.email, role)
    DB.withTransaction { implicit conn =>
      OrganizationLog.create(createdBy, org, message)
      MembershipRequest.softDelete(createdBy, guid)
      Membership.upsert(createdBy, org, user, role)
    }
  }

  /**
   * Declines this request. The request will be deleted and the
   * action logged.
   */
  def decline(createdBy: User) {
    val message = "Declined membership request for %s to join as %s".format(user.email, role)
    DB.withTransaction { implicit conn =>
      OrganizationLog.create(createdBy, org, message)
      MembershipRequest.softDelete(createdBy, guid)
    }
  }

}

object MembershipRequest {

  private val BaseQuery = """
    select membership_requests.guid::varchar,
           membership_requests.role,
           membership_requests.created_at::varchar,
           organizations.guid::varchar as organization_guid,
           organizations.name as organization_name,
           organizations.key as organization_key,
           users.guid::varchar as user_guid,
           users.email as user_email,
           users.name as user_name,
           users.image_url as user_image_url
      from membership_requests
      join organizations on organizations.guid = membership_requests.organization_guid
      join users on users.guid = membership_requests.user_guid
     where membership_requests.deleted_at is null
  """

  def upsert(createdBy: User, organization: Organization, user: User, role: String): MembershipRequest = {
    findByOrganizationAndUserAndRole(organization, user, role) match {
      case Some(r: MembershipRequest) => r
      case None => {
        create(createdBy, organization, user, role)
      }
    }
  }

  private def create(createdBy: User, organization: Organization, user: User, role: String): MembershipRequest = {
    val guid = UUID.randomUUID
    DB.withConnection { implicit c =>
      SQL("""
          insert into membership_requests
          (guid, organization_guid, user_guid, role, created_by_guid)
          values
          ({guid}::uuid, {organization_guid}::uuid, {user_guid}::uuid, {role}, {created_by_guid}::uuid)
          """).on('guid -> guid,
                  'organization_guid -> organization.guid,
                  'user_guid -> user.guid,
                  'role -> role,
                  'created_by_guid -> createdBy.guid).execute()
    }

    findByGuid(guid.toString).getOrElse {
      sys.error("Failed to create membership_request")
    }
  }

  def softDelete(user: User, guid: String) {
    DB.withConnection { implicit c =>
      SQL("""
          update membership_requests set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now() where membership_requests.guid = {guid}::uuid and deleted_at is null
          """).on('deleted_by_guid -> user.guid, 'guid -> guid).execute()
    }
  }

  def findByGuid(guid: String): Option[MembershipRequest] = {
    findAll(guid = Some(guid.toString), limit = 1).headOption
  }

  def findAllForOrganization(org: Organization): Seq[MembershipRequest] = {
    findAll(organization_guid = Some(org.guid))
  }

  def findAllForUser(user: User): Seq[MembershipRequest] = {
    findAll(user_guid = Some(user.guid))
  }

  def findAllPendingReview(user: User): Seq[MembershipRequest] = {
    findAll(can_be_reviewed_by = Some(user))
  }

  private def findByOrganizationAndUserAndRole(org: Organization, user: User, role: String): Option[MembershipRequest] = {
    findAll(organization_guid = Some(org.guid),
            user_guid = Some(user.guid),
            role = Some(role),
            limit = 1).headOption
  }

  def findAll(user: Option[User] = None,
              guid: Option[String] = None,
              organization_guid: Option[String] = None,
              user_guid: Option[String] = None,
              role: Option[String] = None,
              can_be_reviewed_by: Option[User] = None,
              limit: Int = 50,
              offset: Int = 0): Seq[MembershipRequest] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      user.map { u => "and (membership_requests.user_guid = {user_guid} or membership_requests.organization_guid in (select organization_guid from memberships where deleted_at is null and user_guid = {user_guid}" },
      guid.map { v => "and membership_requests.guid = {guid}::uuid" },
      organization_guid.map { v => "and membership_requests.organization_guid = {organization_guid}::uuid" },
      user_guid.map { v => "and membership_requests.user_guid = {user_guid}::uuid" },
      role.map { v => "and membership_requests.role = {role}" },
      can_be_reviewed_by.map { user =>
        """
         and membership_requests.organization_guid in
             (select organization_guid
                from memberships
               where deleted_at is null
                 and user_guid = {reviewing_user_guid}::uuid)
                 and role = 'admin'
        """
      },
      Some(s"order by membership_requests.created_at desc limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq(
      user.map { u => 'user_guid -> toParameterValue(u.guid) },
      guid.map { v => 'guid -> toParameterValue(v) },
      organization_guid.map { v => 'organization_guid -> toParameterValue(v) },
      user_guid.map { v => 'user_guid -> toParameterValue(v) },
      can_be_reviewed_by.map { user => 'reviewing_user_guid -> toParameterValue(user.guid) },
      role.map { role => 'role -> toParameterValue(role) }
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        MembershipRequest(guid = row[String]("guid"),
                          created_at = row[String]("created_at"),
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
