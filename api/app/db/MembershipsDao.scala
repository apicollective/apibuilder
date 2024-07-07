package db

import anorm._
import io.apibuilder.api.v0.models.{Organization, User}
import io.apibuilder.common.v0.models.{Audit, MembershipRole, ReferenceGuid}
import io.apibuilder.task.v0.models.EmailDataMembershipCreated
import io.flow.postgresql.Query
import play.api.db._
import processor.EmailProcessorQueue

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
  subscriptionsDao: SubscriptionsDao
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
           ${AuditsDao.queryCreationDefaultingUpdatedAt("memberships")},
           memberships.organization_guid,
           memberships.user_guid
      from memberships
  """)

  def upsert(createdBy: UUID, organization: Organization, user: User, role: MembershipRole): InternalMembership = {
    val membership = findByOrganizationAndUserAndRole(Authorization.All, organization, user, role) match {
      case Some(r) => r
      case None => create(createdBy, organization, user, role)
    }

    // If we made this user an admin, and s/he already exists as a
    // member, remove the member role - this is akin to an upgrade
    // in membership from member to admin.
    if (role == MembershipRole.Admin) {
      findByOrganizationAndUserAndRole(Authorization.All, organization, user, MembershipRole.Member).foreach { membership =>
        softDelete(user, membership)
      }
    }

    membership
  }

  private[db] def create(createdBy: UUID, organization: Organization, user: User, role: MembershipRole): InternalMembership = {
    db.withTransaction { implicit c =>
      create(c, createdBy, organization, user, role)
    }
  }

  private[db] def create(implicit c: java.sql.Connection, createdBy: UUID, organization: Organization, user: User, role: MembershipRole): InternalMembership = {
    val guid = UUID.randomUUID

    SQL(InsertQuery).on(
      "guid" -> guid,
      "organization_guid" -> organization.guid,
      "user_guid" -> user.guid,
      "role" -> role.toString,
      "created_by_guid" -> createdBy
    ).execute()

    emailQueue.queueWithConnection(c, EmailDataMembershipCreated(guid))

    BaseQuery.equals("guid", guid)
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
  def softDelete(user: User, membership: InternalMembership): Unit = {
    subscriptionsDao.deleteSubscriptionsRequiringAdmin(user, membership.organizationGuid, membership.userGuid)
    dbHelpers.delete(user, membership.guid)
  }

  def isUserAdmin(
    user: User,
    organization: Organization
  ): Boolean = {
    findByOrganizationAndUserAndRole(Authorization.All, organization, user, MembershipRole.Admin) match {
      case None => false
      case Some(_) => true
    }
  }

  def isUserMember(
    user: User,
    organization: Organization
  ): Boolean = {
    findAll(
      Authorization.All,
      organizationGuid = Some(organization.guid),
      userGuid = Some(user.guid),
      limit = Some(1)
    ).headOption match {
      case None => false
      case Some(_) => true
    }
  }

  def findByOrganizationAndUserAndRole(
    authorization: Authorization,
    organization: Organization,
    user: User,
    role: MembershipRole
  ): Option[InternalMembership] = {
    findAll(authorization, organizationGuid = Some(organization.guid), userGuid = Some(user.guid), role = Some(role), limit = Some(1)).headOption
  }

  def findByOrganizationAndUserAndRoles(
    authorization: Authorization,
    organization: Organization,
    user: User,
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
    db.withConnection { implicit c =>
      BaseQuery.
        equals("memberships.guid", guid).
        equals("memberships.organization_guid", organizationGuid).
        equals("memberships.user_guid", userGuid).
        and(
          organizationKey.map { _ =>
            "memberships.organization_guid = (select guid from organizations where deleted_at is null and key = {organization_key})"
          }
        ).bind("organization_key", organizationKey).
        equals("memberships.role", role.map(_.toString)).
        optionalIn("memberships.role", roles.map(_.map(_.toString))).
        and(isDeleted.map(Filters.isDeleted("memberships", _))).
        orderBy("lower(users.name), lower(users.email)").
        optionalLimit(limit).
        offset(offset).
        anormSql().as(parser.*)
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
