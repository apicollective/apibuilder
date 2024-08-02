package db

import anorm._
import io.apibuilder.api.v0.models.User
import io.apibuilder.common.v0.models.{Audit, MembershipRole, ReferenceGuid}
import io.apibuilder.task.v0.models.EmailDataMembershipCreated
import io.flow.postgresql.Query
import play.api.db._
import processor.EmailProcessorQueue
import util.OptionalQueryFilter

import java.util.UUID
import javax.inject.{Inject, Singleton}

case class InternalMembership(
                             guid: UUID,
                             role: MembershipRole,
                             audit: Audit,
                             organizationGuid: UUID,
                             userGuid: UUID
                             )

@Singleton
class MembershipsDao @Inject() (
  @NamedDatabase("default") db: Database,
  emailQueue: EmailProcessorQueue,
  subscriptionsDao: InternalSubscriptionsDao
) {

  private val dbHelpers = DbHelpers(db, "memberships")

  private val InsertQuery = """
    insert into memberships
    (guid, organization_guid, user_guid, role, created_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {user_guid}::uuid, {role}, {created_by_guid}::uuid)
  """

  private val BaseQuery = Query(s"""
    select memberships.guid,
           memberships.role,
           memberships.organization_guid,
           memberships.user_guid,
           ${AuditsDao.queryCreationDefaultingUpdatedAt("memberships")}
      from memberships
      join users on users.guid = memberships.user_guid
  """)

  def upsert(createdBy: UUID, org: OrganizationReference, user: UserReference, role: MembershipRole): InternalMembership = {
    val membership = findByOrganizationAndUserAndRole(Authorization.All, org, user, role) match {
      case Some(r) => r
      case None => create(createdBy, org, user, role)
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
    db.withTransaction { implicit c =>
      create(c, createdBy, org, user, role)
    }
  }

  private[db] def create(implicit c: java.sql.Connection, createdBy: UUID, org: OrganizationReference, user: UserReference, role: MembershipRole): InternalMembership = {
    val guid = UUID.randomUUID

    SQL(InsertQuery).on(
      "guid" -> guid,
      "organization_guid" -> org.guid,
      "user_guid" -> user.guid,
      "role" -> role.toString,
      "created_by_guid" -> createdBy
    ).execute()

    emailQueue.queueWithConnection(c, EmailDataMembershipCreated(guid))

    BaseQuery.equals("memberships.guid", guid)
      .as(parser.*)
      .headOption
      .getOrElse {
        sys.error("Failed to create membership")
      }
    }

  /**
    * Deletes a membership record. Also removes the user from any
    * publication subscriptions that require the administrative role
    * for this org.
    */
  def softDelete(user: UserReference, membership: InternalMembership): Unit = {
    subscriptionsDao.deleteSubscriptionsRequiringAdmin(user, membership.organizationGuid, membership.userGuid)
    dbHelpers.delete(user.guid, membership.guid)
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

    db.withConnection { implicit c =>
      filters.foldLeft(BaseQuery) { case (q, f) => f.filter(q) }.
        equals("memberships.guid", guid).
        equals("memberships.organization_guid", organizationGuid).
        equals("memberships.user_guid", userGuid).
        equals("memberships.role", role.map(_.toString)).
        optionalIn("memberships.role", roles.map(_.map(_.toString))).
        and(isDeleted.map(Filters.isDeleted("memberships", _))).
        orderBy("lower(users.name), lower(users.email)").
        optionalLimit(limit).
        offset(offset).
        as(parser.*)
    }
  }

  private val parser: RowParser[InternalMembership] = {
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
        InternalMembership(
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
