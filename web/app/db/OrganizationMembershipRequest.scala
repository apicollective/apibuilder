package db

import lib.Constants
import anorm._
import play.api.db._
import play.api.Play.current
import java.sql.Timestamp
import java.util.UUID

case class MembershipRequestQuery(guid: Option[UUID] = None,
                                              org: Option[Organization] = None,
                                              user: Option[User] = None,
                                              role: Option[String] = None,
                                              canBeApprovedBy: Option[User] = None,
                                              limit: Int = 50,
                                              offset: Int = 0)


case class MembershipRequest(guid: UUID,
                                         createdAt: String, // TODO Timestamp type
                                         org: Organization,
                                         user: User,
                                         role: String) {

  /**
   * Approves this request. The request will be deleted, the
   * action logged, and the * user added to the organization
   * in this role if not already in this role for this org.
   */
  def approve(createdBy: User) {
    val message = "Approved membership request for %s to join as %s".format(user.email, role)
    DB.withTransaction { implicit conn =>
      MembershipLog.create(createdBy, org, message)
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
      MembershipLog.create(createdBy, org, message)
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

    findByGuid(guid).getOrElse {
      sys.error("Failed to create membership_request")
    }
  }

  def softDelete(user: User, guid: UUID) {
    DB.withConnection { implicit c =>
      SQL("""
          update membership_requests set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now() where membership_requests.guid = {guid}::uuid and deleted_at is null
          """).on('deleted_by_guid -> user.guid, 'guid -> guid).execute()
    }
  }

  def findByGuid(guid: UUID): Option[MembershipRequest] = {
    findAll(MembershipRequestQuery(guid = Some(guid), limit = 1)).headOption
  }

  def findAllForOrganization(org: Organization): Seq[MembershipRequest] = {
    findAll(MembershipRequestQuery(org = Some(org)))
  }

  def findAllForUser(user: User): Seq[MembershipRequest] = {
    findAll(MembershipRequestQuery(user = Some(user)))
  }

  def findAllPendingApproval(user: User): Seq[MembershipRequest] = {
    findAll(MembershipRequestQuery(canBeApprovedBy = Some(user)))
  }

  private def findByOrganizationAndUserAndRole(org: Organization, user: User, role: String): Option[MembershipRequest] = {
    findAll(MembershipRequestQuery(org = Some(org),
                                               user = Some(user),
                                               role = Some(role),
                                               limit = 1)).headOption
  }

  def findAll(query: MembershipRequestQuery): Seq[MembershipRequest] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      query.guid.map { v => "and membership_requests.guid = {guid}::uuid" },
      query.org.map { v => "and membership_requests.organization_guid = {organization_guid}::uuid" },
      query.user.map { v => "and membership_requests.user_guid = {user_guid}::uuid" },
      query.role.map { v => "and membership_requests.role = {role}" },
      query.canBeApprovedBy.map { user =>
        """
         and membership_requests.organization_guid in
             (select organization_guid
                from memberships
                 where deleted_at is null
                 and role = 'admin'
                 and user_guid = {approving_user_guid}::uuid)
        """
      },
      Some(s"order by membership_requests.created_at desc limit ${query.limit} offset ${query.offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq(
      query.guid.map { v => 'guid -> toParameterValue(v) },
      query.org.map { org => 'organization_guid -> toParameterValue(org.guid) },
      query.user.map { user => 'user_guid -> toParameterValue(user.guid) },
      query.canBeApprovedBy.map { user => 'approving_user_guid -> toParameterValue(user.guid) },
      query.role.map { role => 'role -> toParameterValue(role) }
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        MembershipRequest(guid = UUID.fromString(row[String]("guid")),
                                      createdAt = row[String]("created_at"),
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
