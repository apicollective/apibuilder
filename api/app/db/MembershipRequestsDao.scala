package db

import anorm._
import io.apibuilder.api.v0.models.{MembershipRequest, Organization, User}
import io.apibuilder.common.v0.models.{Audit, MembershipRole, ReferenceGuid}
import io.apibuilder.task.v0.models.{EmailDataMembershipRequestAccepted, EmailDataMembershipRequestCreated, EmailDataMembershipRequestDeclined}
import io.flow.postgresql.Query
import play.api.db._
import processor.EmailProcessorQueue
import util.OptionalQueryFilter

import java.util.UUID
import javax.inject.{Inject, Singleton}

case class InternalMembershipRequest(
                                      guid: UUID,
                                      role: MembershipRole,
                                      organizationGuid: UUID,
                                      userGuid: UUID,
                                      audit: Audit)

@Singleton
class MembershipRequestsDao @Inject() (
  @NamedDatabase("default") db: Database,
  emailQueue: EmailProcessorQueue,
  membershipsDao: MembershipsDao,
  organizationLogsDao: OrganizationLogsDao
) {

  private val dbHelpers = DbHelpers(db, "membership_requests")

  // TODO: Properly select domains
  private val BaseQuery = Query(s"""
    select guid,
           role,
           created_at::text,
           organization_guid,
           user_guid,
           ${AuditsDao.queryCreationDefaultingUpdatedAt("membership_requests")}
      from membership_requests
  """)

  private val InsertQuery = """
   insert into membership_requests
   (guid, organization_guid, user_guid, role, created_by_guid)
   values
   ({guid}::uuid, {organization_guid}::uuid, {user_guid}::uuid, {role}, {created_by_guid}::uuid)
  """

  def upsert(createdBy: User, organization: Organization, user: User, role: MembershipRole): InternalMembershipRequest = {
    findByOrganizationAndUserGuidAndRole(Authorization.All, organization, user.guid, role) match {
      case Some(r: InternalMembershipRequest) => r
      case None => {
        create(createdBy, organization, user, role)
      }
    }
  }

  private[db] def create(createdBy: User, organization: Organization, user: User, role: MembershipRole): InternalMembershipRequest = {
    val guid = UUID.randomUUID
    db.withTransaction { implicit c =>
      SQL(InsertQuery).on(
        "guid" -> guid,
        "organization_guid" -> organization.guid,
        "user_guid" -> user.guid,
        "role" -> role.toString,
        "created_by_guid" -> createdBy.guid
      ).execute()
      emailQueue.queueWithConnection(c, EmailDataMembershipRequestCreated(guid))
    }

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
    doAccept(createdBy.guid, request, s"Accepted membership request for ${request.user.email} to join as ${request.role}")
  }

  def acceptViaEmailVerification(createdBy: UUID, request: MembershipRequest, email: String): Unit = {
    doAccept(createdBy, request, s"$email joined as ${request.role} by verifying their email address")
  }

  private def doAccept(createdBy: UUID, request: MembershipRequest, message: String): Unit = {
    db.withTransaction { implicit c =>
      organizationLogsDao.create(createdBy, request.organization, message)
      dbHelpers.delete(c, createdBy, request.guid)
      membershipsDao.upsert(createdBy, request.organization, request.user, request.role)
      emailQueue.queueWithConnection(c, EmailDataMembershipRequestAccepted(request.organization.guid, request.user.guid, request.role))
    }
  }

  /**
   * Declines this request. The request will be deleted and the
   * action logged.
   */
  def decline(createdBy: User, request: MembershipRequest): Unit = {
    assertUserCanReview(createdBy, request)

    val message = s"Declined membership request for ${request.user.email} to join as ${request.role}"
    db.withTransaction { implicit c =>
      organizationLogsDao.create(createdBy.guid, request.organization, message)
      softDelete(createdBy, request)
      emailQueue.queueWithConnection(c, EmailDataMembershipRequestDeclined(request.organization.guid, request.user.guid))
    }
  }

  private def assertUserCanReview(user: User, request: MembershipRequest): Unit = {
    require(
      membershipsDao.isUserAdmin(user, request.organization),
      s"User[${user.guid}] is not an administrator of org[${request.organization.guid}]"
    )
  }

  def softDelete(user: User, membershipRequest: MembershipRequest): Unit = {
    dbHelpers.delete(user, membershipRequest.guid)
  }

  def findByOrganizationAndUserGuidAndRole(
    authorization: Authorization,
    org: Organization,
    userGuid: UUID,
    role: MembershipRole
  ): Option[InternalMembershipRequest] = {
    findAll(
      authorization,
      organizationGuid = Some(org.guid),
      userGuid = Some(userGuid),
      role = Some(role),
      limit = 1
    ).headOption
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[InternalMembershipRequest] = {
    findAll(authorization, guid = Some(guid)).headOption
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    organizationGuid: Option[UUID] = None,
    organizationKey: Option[String] = None,
    userGuid: Option[UUID] = None,
    role: Option[MembershipRole] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[InternalMembershipRequest] = {
    // TODO Implement authorization

    val filters = List(
      new OptionalQueryFilter(organizationKey) {
        override def filter(q: Query, value: String): Query = {
          q.in("organization_guid", Query("select guid from organizations").isNull("deleted_at").equals("key", organizationKey))
        }
      }
    )

    db.withConnection { implicit c =>
      filters.foldLeft(BaseQuery) { case (q, f) => f.filter(q) }.
        equals("guid", guid).
        equals("organization_guid", organizationGuid).
        equals("user_guid", userGuid).
        equals("role", role.map(_.toString)).
        and(isDeleted.map(Filters.isDeleted("membership_requests", _))).
        orderBy("created_at desc").
        limit(limit).
        offset(offset).
        as(parser.*)
    }
  }

  private val parser: RowParser[InternalMembershipRequest] = {
    import org.joda.time.DateTime

    SqlParser.get[UUID]("guid") ~
      SqlParser.str("role") ~
      SqlParser.get[DateTime]("created_at") ~
      SqlParser.get[UUID]("created_by_guid") ~
      SqlParser.get[DateTime]("updated_at") ~
      SqlParser.get[UUID]("updated_by_guid") ~
      SqlParser.get[UUID]("organization_guid") ~
      SqlParser.get[UUID]("user_guid") map {
      case guid ~ role ~ createdAt ~ createdByGuid ~ updatedAt ~ updatedByGuid ~ organizationGuid ~ userGuid => {
        InternalMembershipRequest(
          guid = guid,
          role = MembershipRole.apply(role),
          organizationGuid = organizationGuid,
          userGuid = userGuid,
          audit = Audit(
            createdAt = createdAt,
            createdBy = ReferenceGuid(createdByGuid),
            updatedAt = updatedAt,
            updatedBy = ReferenceGuid(updatedByGuid),
          )
        )
      }
    }
  }
}
