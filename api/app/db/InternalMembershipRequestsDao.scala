package db

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec
import cats.implicits.*
import db.generated.MembershipRequestsDao
import io.apibuilder.api.v0.models.{MembershipRequest, User}
import io.apibuilder.common.v0.models.{Audit, MembershipRole, ReferenceGuid}
import io.apibuilder.task.v0.models.{EmailDataMembershipRequestAccepted, EmailDataMembershipRequestCreated, EmailDataMembershipRequestDeclined}
import io.flow.postgresql.{OrderBy, Query}
import play.api.db.*
import processor.EmailProcessorQueue
import util.OptionalQueryFilter

import java.util.UUID
import javax.inject.Inject

case class InternalMembershipRequest(db: generated.MembershipRequest) {
  val guid: UUID = db.guid
  val role: MembershipRole = MembershipRole(db.role)
  val organizationGuid: UUID = db.organizationGuid
  val userGuid: UUID = db.userGuid
}

class InternalMembershipRequestsDao @Inject()(
                                               dao: MembershipRequestsDao,
                                               emailQueue: EmailProcessorQueue,
                                               membershipsDao: InternalMembershipsDao,
                                               organizationLogsDao: OrganizationLogsDao
) {

  def upsert(createdBy: InternalUser, organization: InternalOrganization, user: InternalUser, role: MembershipRole): InternalMembershipRequest = {
    findByOrganizationAndUserGuidAndRole(Authorization.All, organization, user.guid, role).getOrElse {
      create(createdBy, organization, user, role)
    }
  }

  private[db] def create(createdBy: InternalUser, organization: InternalOrganization, user: InternalUser, role: MembershipRole): InternalMembershipRequest = {
    val guid = dao.db.withTransaction { c =>
      val guid = dao.insert(c, createdBy.guid, generated.MembershipRequestForm(
        userGuid = user.guid,
        organizationGuid = organization.guid,
        role = role.toString,
      ))
      emailQueue.queueWithConnection(c, EmailDataMembershipRequestCreated(guid))
      guid
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
  def accept(createdBy: InternalUser, request: MembershipRequest): Unit = {
    assertUserCanReview(createdBy, request)
    doAccept(createdBy.guid, request, s"Accepted membership request for ${request.user.email} to join as ${request.role}")
  }

  def acceptViaEmailVerification(createdBy: UUID, request: MembershipRequest, email: String): Unit = {
    doAccept(createdBy, request, s"$email joined as ${request.role} by verifying their email address")
  }

  private def doAccept(createdBy: UUID, request: MembershipRequest, message: String): Unit = {
    dao.db.withTransaction { implicit c =>
      organizationLogsDao.create(createdBy, OrganizationReference(request.organization), message)
      dao.deleteByGuid(c, createdBy, request.guid)
      membershipsDao.upsert(createdBy, OrganizationReference(request.organization), UserReference(request.user), request.role)
      emailQueue.queueWithConnection(c, EmailDataMembershipRequestAccepted(request.organization.guid, request.user.guid, request.role))
    }
  }

  /**
   * Declines this request. The request will be deleted and the
   * action logged.
   */
  def decline(createdBy: InternalUser, request: MembershipRequest): Unit = {
    assertUserCanReview(createdBy, request)

    val message = s"Declined membership request for ${request.user.email} to join as ${request.role}"
    dao.db.withTransaction { implicit c =>
      organizationLogsDao.create(createdBy.guid, OrganizationReference(request.organization), message)
      dao.deleteByGuid(c, createdBy.guid, request.guid)
      emailQueue.queueWithConnection(c, EmailDataMembershipRequestDeclined(request.organization.guid, request.user.guid))
    }
  }

  private def assertUserCanReview(user: InternalUser, request: MembershipRequest): Unit = {
    require(
      membershipsDao.isUserAdmin(user, OrganizationReference(request.organization)),
      s"User[${user.guid}] is not an administrator of org[${request.organization.guid}]"
    )
  }

  def softDelete(user: InternalUser, membershipRequest: InternalMembershipRequest): Unit = {
    dao.delete(user.guid, membershipRequest.db)
  }

  def findByOrganizationAndUserGuidAndRole(
    authorization: Authorization,
    org: InternalOrganization,
    userGuid: UUID,
    role: MembershipRole
  ): Option[InternalMembershipRequest] = {
    findAll(
      authorization,
      organizationGuid = Some(org.guid),
      userGuid = Some(userGuid),
      role = Some(role),
      limit = Some(1)
    ).headOption
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[InternalMembershipRequest] = {
    findAll(authorization, guid = Some(guid), limit = Some(1)).headOption
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    organizationGuid: Option[UUID] = None,
    organizationKey: Option[String] = None,
    userGuid: Option[UUID] = None,
    role: Option[MembershipRole] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long],
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

    dao.findAll(
      guid = guid,
      organizationGuid = organizationGuid,
      userGuid = userGuid,
      limit = limit,
      offset = offset,
      orderBy = Some(OrderBy("created_at")),
    )( using (q: Query) => {
        filters.foldLeft(q) { case (q, f) => f.filter(q) }
        .and(isDeleted.map(Filters.isDeleted("membership_requests", _)))
        .equals("role", role.map(_.toString))
    }).map(InternalMembershipRequest(_))
  }
}
