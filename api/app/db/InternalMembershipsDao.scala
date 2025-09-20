package db

import anorm.*
import db.generated.MembershipsDao
import io.apibuilder.api.v0.models.User
import io.apibuilder.common.v0.models.{Audit, MembershipRole, ReferenceGuid}
import io.apibuilder.task.v0.models.EmailDataMembershipCreated
import io.flow.postgresql.Query
import play.api.db.*
import processor.EmailProcessorQueue
import util.OptionalQueryFilter

import java.util.UUID
import javax.inject.{Inject, Singleton}

case class InternalMembership(db: generated.Membership) {
  val guid: UUID = db.guid
  val role: MembershipRole = MembershipRole(db.role)
  val organizationGuid: UUID = db.organizationGuid
  val userGuid: UUID = db.userGuid
}

class InternalMembershipsDao @Inject()(
  dao: MembershipsDao,
  emailQueue: EmailProcessorQueue,
  subscriptionsDao: InternalSubscriptionsDao
) {

  def upsert(createdBy: UUID, org: OrganizationReference, user: UserReference, role: MembershipRole): InternalMembership = {
    val membership = findByOrganizationAndUserAndRole(Authorization.All, org, user, role).getOrElse {
      create(createdBy, org, user, role)
    }

    // If we made this user an admin, and s/he already exists as a
    // member, remove the member role - this is akin to an upgrade
    // in membership from member to admin.
    if (role == MembershipRole.Admin) {
      findByOrganizationAndUserAndRole(Authorization.All, org, user, MembershipRole.Member).foreach { membership =>
        softDelete(user, membership)
      }
    }

    membership
  }

  private[db] def create(createdBy: UUID, org: OrganizationReference, user: UserReference, role: MembershipRole): InternalMembership = {
    dao.db.withTransaction { implicit c =>
      create(c, createdBy, org, user, role)
    }
  }

  private[db] def create(c: java.sql.Connection, createdBy: UUID, org: OrganizationReference, user: UserReference, role: MembershipRole): InternalMembership = {
    val guid = dao.insert(c, createdBy, generated.MembershipForm(
      userGuid = user.guid,
      organizationGuid = org.guid,
      role = role.toString,
    ))

    emailQueue.queueWithConnection(c, EmailDataMembershipCreated(guid))

    InternalMembership(
      dao.findByGuidWithConnection(c, guid).getOrElse {
        sys.error("Failed to create membership")
      }
    )
  }

  /**
    * Deletes a membership record. Also removes the user from any
    * publication subscriptions that require the administrative role
    * for this org.
    */
  def softDelete(user: UserReference, membership: InternalMembership): Unit = {
    // TODO: Wrap in transaction
    subscriptionsDao.deleteSubscriptionsRequiringAdmin(user, membership.organizationGuid, membership.userGuid)
    dao.delete(user.guid, membership.db)
  }

  def isUserAdmin(
    user: InternalUser,
    organization: OrganizationReference
  ): Boolean = {
    isUserAdmin(userGuid = user.guid, organizationGuid = organization.guid)
  }

  def isUserAdmin(
                   userGuid: UUID,
                   organizationGuid: UUID
                 ): Boolean = {
    findByOrganizationGuidAndUserGuidAndRole(Authorization.All, organizationGuid, userGuid, MembershipRole.Admin) match {
      case None => false
      case Some(_) => true
    }
  }

  def isUserMember(
    user: InternalUser,
    organization: OrganizationReference
  ): Boolean = {
    isUserMember(userGuid = user.guid, organizationGuid = organization.guid)
  }

  def isUserMember(
                    userGuid: UUID,
                    organizationGuid: UUID
                  ): Boolean = {
    findAll(
      Authorization.All,
      organizationGuid = Some(organizationGuid),
      userGuid = Some(userGuid),
      limit = Some(1)
    ).headOption match {
      case None => false
      case Some(_) => true
    }
  }

  def findByOrganizationAndUserAndRole(
    authorization: Authorization,
    org: OrganizationReference,
    user: UserReference,
    role: MembershipRole
  ): Option[InternalMembership] = {
    findByOrganizationGuidAndUserGuidAndRole(
      authorization,
      organizationGuid = org.guid,
      userGuid = user.guid,
      role = role
    )
  }

  private def findByOrganizationGuidAndUserGuidAndRole(
                                        authorization: Authorization,
                                        organizationGuid: UUID,
                                        userGuid: UUID,
                                        role: MembershipRole
                                      ): Option[InternalMembership] = {

    findAll(authorization, organizationGuid = Some(organizationGuid), userGuid = Some(userGuid), role = Some(role), limit = Some(1)).headOption
  }

  def findByOrganizationAndUserAndRoles(
    authorization: Authorization,
    organization: OrganizationReference,
    user: InternalUser,
    roles: Seq[MembershipRole]
  ): Seq[InternalMembership] = {
    findAll(authorization, organizationGuid = Some(organization.guid), userGuid = Some(user.guid), roles = Some(roles), limit = None)
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[InternalMembership] = {
    findAll(authorization, guid = Some(guid), limit = Some(1)).headOption
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    organizationGuid: Option[UUID] = None,
    organizationKey: Option[String] = None,
    userGuid: Option[UUID] = None,
    role: Option[MembershipRole] = None,
    roles: Option[Seq[MembershipRole]] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[InternalMembership] = {
    // TODO Implement authorization
    val filters = List(
      new OptionalQueryFilter(organizationKey) {
        override def filter(q: Query, value: String): Query = {
          q.in("memberships.organization_guid", Query("select guid from organizations").isNull("deleted_at").equals("key", organizationKey))
        }
      }
    )

    dao.findAll(
      guid = guid,
      organizationGuid = organizationGuid,
      userGuid = userGuid,
      limit = limit,
      offset = offset,
    )( using (q: Query) => {
      filters.foldLeft(q) { case (q, f) => f.filter(q) }
        .equals("memberships.role", role.map(_.toString))
        .optionalIn("memberships.role", roles.map(_.map(_.toString)))
        .and(isDeleted.map(Filters.isDeleted("memberships", _)))
    }).map(InternalMembership(_))
  }
}
