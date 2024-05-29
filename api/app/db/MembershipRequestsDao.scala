package db

import io.apibuilder.api.v0.models.{MembershipRequest, Organization, User}
import io.flow.postgresql.Query
import lib.Role
import anorm._
import javax.inject.{Inject, Named, Singleton}
import play.api.db._
import java.util.UUID

@Singleton
class MembershipRequestsDao @Inject() (
  @Named("main-actor") mainActor: akka.actor.ActorRef,
  @NamedDatabase("default") db: Database,
  membershipsDao: MembershipsDao,
  organizationLogsDao: OrganizationLogsDao
) {

  private[this] val dbHelpers = DbHelpers(db, "membership_requests")

  // TODO: Properly select domains
  private[this] val BaseQuery = Query(s"""
    select membership_requests.guid,
           membership_requests.role,
           membership_requests.created_at::text,
           ${AuditsDao.queryCreationDefaultingUpdatedAt("membership_requests")},
           organizations.guid as organization_guid,
           organizations.name as organization_name,
           organizations.key as organization_key,
           organizations.visibility as organization_visibility,
           organizations.namespace as organization_namespace,
           '[]' as organization_domains,
           ${AuditsDao.queryWithAlias("organizations", "organization")},
           users.guid as user_guid,
           users.email as user_email,
           users.nickname as user_nickname,
           users.name as user_name,
           ${AuditsDao.queryWithAlias("users", "user")}
      from membership_requests
      join organizations on organizations.guid = membership_requests.organization_guid
      join users on users.guid = membership_requests.user_guid
  """)

  private[this] val InsertQuery = """
   insert into membership_requests
   (guid, organization_guid, user_guid, role, created_by_guid)
   values
   ({guid}::uuid, {organization_guid}::uuid, {user_guid}::uuid, {role}, {created_by_guid}::uuid)
  """

  def upsert(createdBy: User, organization: Organization, user: User, role: Role): MembershipRequest = {
    findByOrganizationAndUserGuidAndRole(Authorization.All, organization, user.guid, role) match {
      case Some(r: MembershipRequest) => r
      case None => {
        create(createdBy, organization, user, role)
      }
    }
  }

  private[db] def create(createdBy: User, organization: Organization, user: User, role: Role): MembershipRequest = {
    val guid = UUID.randomUUID
    db.withConnection { implicit c =>
      SQL(InsertQuery).on(
        "guid" -> guid,
        "organization_guid" -> organization.guid,
        "user_guid" -> user.guid,
        "role" -> role.key,
        "created_by_guid" -> createdBy.guid
      ).execute()
    }

    mainActor ! actors.MainActor.Messages.MembershipRequestCreated(guid)

    findByGuid(Authorization.All, guid).getOrElse {
      sys.error("Failed to create membership_request")
    }
  }

  /**
   * Accepts this request. The request will be deleted, the
   * action logged, and the user added to the organization
   * in this role if not already in this role for this org.
   */
  def accept(createdBy: User, request: MembershipRequest): Unit = {
    assertUserCanReview(createdBy, request)
    val r = Role.fromString(request.role).getOrElse {
      sys.error(s"Invalid role[${request.role}]")
    }
    doAccept(createdBy.guid, request, s"Accepted membership request for ${request.user.email} to join as ${r.name}")
  }

  private[db] def acceptViaEmailVerification(createdBy: UUID, request: MembershipRequest, email: String): Unit = {
    val r = Role.fromString(request.role).getOrElse {
      sys.error(s"Invalid role[${request.role}]")
    }
    doAccept(createdBy, request, s"$email joined as ${r.name} by verifying their email address")
  }

  private[this] def doAccept(createdBy: UUID, request: MembershipRequest, message: String): Unit = {
    val r = Role.fromString(request.role).getOrElse {
      sys.error(s"Invalid role[${request.role}]")
    }

    db.withTransaction { implicit conn =>
      organizationLogsDao.create(createdBy, request.organization, message)
      dbHelpers.delete(conn, createdBy, request.guid)
      membershipsDao.upsert(createdBy, request.organization, request.user, r)
    }

    mainActor ! actors.MainActor.Messages.MembershipRequestAccepted(request.organization.guid, request.user.guid, r)
  }

  /**
   * Declines this request. The request will be deleted and the
   * action logged.
   */
  def decline(createdBy: User, request: MembershipRequest): Unit = {
    assertUserCanReview(createdBy, request)
    val r = Role.fromString(request.role).getOrElse {
      sys.error(s"Invalid role[${request.role}]")
    }

    val message = s"Declined membership request for ${request.user.email} to join as ${r.name}"
    db.withTransaction { implicit conn =>
      organizationLogsDao.create(createdBy.guid, request.organization, message)
      softDelete(createdBy, request)
    }

    mainActor ! actors.MainActor.Messages.MembershipRequestDeclined(request.organization.guid, request.user.guid, r)
  }

  private[this] def assertUserCanReview(user: User, request: MembershipRequest): Unit = {
    require(
      membershipsDao.isUserAdmin(user, request.organization),
      s"User[${user.guid}] is not an administrator of org[${request.organization.guid}]"
    )
  }

  def softDelete(user: User, membershipRequest: MembershipRequest): Unit = {
    dbHelpers.delete(user, membershipRequest.guid)
  }

  private[db] def findByOrganizationAndUserGuidAndRole(
    authorization: Authorization,
    org: Organization,
    userGuid: UUID,
    role: Role
  ): Option[MembershipRequest] = {
    findAll(
      authorization,
      organizationGuid = Some(org.guid),
      userGuid = Some(userGuid),
      role = Some(role.key),
      limit = 1
    ).headOption
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[MembershipRequest] = {
    findAll(authorization, guid = Some(guid)).headOption
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    organizationGuid: Option[UUID] = None,
    organizationKey: Option[String] = None,
    userGuid: Option[UUID] = None,
    role: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[MembershipRequest] = {
    // TODO Implement authorization

    db.withConnection { implicit c =>
      BaseQuery.
        equals("membership_requests.guid", guid).
        equals("membership_requests.organization_guid", organizationGuid).
        equals("membership_requests.user_guid", userGuid).
        and(
          organizationKey.map { _ =>
            "membership_requests.organization_guid = (select guid from organizations where deleted_at is null and key = {organization_key})"
          }
        ).bind("organization_key", organizationKey).
        equals("membership_requests.role", role).
        and(isDeleted.map(Filters.isDeleted("membership_requests", _))).
        orderBy("membership_requests.created_at desc").
        limit(limit).
        offset(offset).
        anormSql().as(
          io.apibuilder.api.v0.anorm.parsers.MembershipRequest.parser().*
        )
    }
  }

}
