package db

import com.gilt.apidoc.models.{Organization, User}
import com.gilt.apidoc.models.json._
import lib.Role
import anorm._
import play.api.db._
import play.api.Play.current
import java.sql.Timestamp
import play.api.libs.json._
import java.util.UUID


case class MembershipRequest(guid: String,
                             created_at: String, // TODO Timestamp type
                             organization: Organization,
                             user: User,
                             role: String) {

  /**
   * Accepts this request. The request will be deleted, the
   * action logged, and the user added to the organization
   * in this role if not already in this role for this org.
   */
  def accept(createdBy: User) {
    assertUserCanReview(createdBy)
    val r = Role.fromString(role).getOrElse {
      sys.error(s"Invalid role[$role]")
    }

    val message = s"Accepted membership request for ${user.email} to join as ${r.name}"
    DB.withTransaction { implicit conn =>
      OrganizationLog.create(createdBy, organization, message)
      MembershipRequest.softDelete(createdBy, this)
      Membership.upsert(createdBy, organization, user, r)
    }
  }

  /**
   * Declines this request. The request will be deleted and the
   * action logged.
   */
  def decline(createdBy: User) {
    assertUserCanReview(createdBy)
    val r = Role.fromString(role).getOrElse {
      sys.error(s"Invalid role[$role]")
    }

    val message = s"Declined membership request for ${user.email} to join as ${r.name}"
    DB.withTransaction { implicit conn =>
      OrganizationLog.create(createdBy, organization, message)
      MembershipRequest.softDelete(createdBy, this)
    }
  }

  private def assertUserCanReview(user: User) {
    require(Membership.isUserAdmin(user, organization),
            s"User[${user.guid}] is not an administrator of org[${organization.guid}]")
  }

}

object MembershipRequest {

  implicit val membershipRequestWrites = Json.writes[MembershipRequest]


  private val BaseQuery = """
    select membership_requests.guid::varchar,
           membership_requests.role,
           membership_requests.created_at::varchar,
           organizations.guid as organization_guid,
           organizations.name as organization_name,
           organizations.key as organization_key,
           users.guid as user_guid,
           users.email as user_email,
           users.name as user_name
      from membership_requests
      join organizations on organizations.guid = membership_requests.organization_guid
      join users on users.guid = membership_requests.user_guid
     where membership_requests.deleted_at is null
  """

  def upsert(createdBy: User, organization: Organization, user: User, role: Role): MembershipRequest = {
    findByOrganizationAndUserAndRole(organization, user, role) match {
      case Some(r: MembershipRequest) => r
      case None => {
        create(createdBy, organization, user, role)
      }
    }
  }

  private[db] def create(createdBy: User, organization: Organization, user: User, role: Role): MembershipRequest = {
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
                  'role -> role.key,
                  'created_by_guid -> createdBy.guid).execute()
    }

    findByGuid(guid.toString).getOrElse {
      sys.error("Failed to create membership_request")
    }
  }

  def softDelete(user: User, membershipRequest: MembershipRequest) {
    SoftDelete.delete("membership_requests", user, membershipRequest.guid)
  }

  private[db] def findByOrganizationAndUserAndRole(org: Organization, user: User, role: Role): Option[MembershipRequest] = {
    findAll(organizationGuid = Some(org.guid),
            userGuid = Some(user.guid),
            role = Some(role.key),
            limit = 1).headOption
  }

  def findByGuid(guid: String): Option[MembershipRequest] = {
    findAll(guid = Some(guid)).headOption
  }

  def findAll(guid: Option[String] = None,
              organizationGuid: Option[UUID] = None,
              organizationKey: Option[String] = None,
              userGuid: Option[UUID] = None,
              role: Option[String] = None,
              limit: Int = 50,
              offset: Int = 0): Seq[MembershipRequest] = {

    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and membership_requests.guid = {guid}::uuid" },
      organizationGuid.map { v => "and membership_requests.organization_guid = {organization_guid}::uuid" },
      organizationKey.map { v => "and membership_requests.organization_guid = (select guid from organizations where deleted_at is null and key = {organization_key})" },
      userGuid.map { v => "and membership_requests.user_guid = {user_guid}::uuid" },
      role.map { v => "and membership_requests.role = {role}" },
      Some(s"order by membership_requests.created_at desc limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _),
      organizationGuid.map('organization_guid -> _.toString),
      organizationKey.map('organization_key -> _),
      userGuid.map('user_guid -> _.toString),
      role.map('role ->_)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        MembershipRequest(
          guid = row[String]("guid"),
          created_at = row[String]("created_at"),
          organization = OrganizationDao.summaryFromRow(row, Some("organization")),
          user = UserDao.fromRow(row, Some("user")),
          role = row[String]("role")
        )
      }.toSeq
    }
  }

}
