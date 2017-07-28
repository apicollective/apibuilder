package db

import io.apibuilder.api.v0.models.{Membership, Organization, User}
import io.apibuilder.api.v0.models.json._
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}
import io.apibuilder.common.v0.models.json._
import lib.Role
import anorm._
import javax.inject.{Inject, Named, Singleton}
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID
import org.joda.time.DateTime

@Singleton
class MembershipsDao @Inject() (
  @Named("main-actor") mainActor: akka.actor.ActorRef,
  organizationsDao: OrganizationsDao,
  subscriptionsDao: SubscriptionsDao,
  usersDao: UsersDao
) {

  private[this] val InsertQuery = """
    insert into memberships
    (guid, organization_guid, user_guid, role, created_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {user_guid}::uuid, {role}, {created_by_guid}::uuid)
  """

  private[this] val BaseQuery = s"""
    select memberships.guid,
           memberships.role,
           ${AuditsDao.queryCreation("memberships")},
           organizations.guid as organization_guid,
           organizations.name as organization_name,
           organizations.key as organization_key,
           organizations.visibility as organization_visibility,
           organizations.namespace as organization_namespace,
           ${AuditsDao.queryWithAlias("organizations", "organization")},
           users.guid as user_guid,
           users.email as user_email,
           users.nickname as user_nickname,
           users.name as user_name,
           ${AuditsDao.queryWithAlias("users", "user")}
      from memberships
      join organizations on organizations.guid = memberships.organization_guid
      join users on users.guid = memberships.user_guid
     where true
  """

  def upsert(createdBy: User, organization: Organization, user: User, role: Role): Membership = {
    val membership = findByOrganizationAndUserAndRole(Authorization.All, organization, user, role) match {
      case Some(r) => r
      case None => create(createdBy, organization, user, role)
    }

    // If we made this user an admin, and s/he already exists as a
    // member, remove the member role - this is akin to an upgrade
    // in membership from member to admin.
    if (role == Role.Admin) {
      findByOrganizationAndUserAndRole(Authorization.All, organization, user, Role.Member).foreach { membership =>
        softDelete(user, membership)
      }
    }

    membership
  }

  private[db] def create(createdBy: User, organization: Organization, user: User, role: Role): Membership = {
    DB.withConnection { implicit c =>
      create(c, createdBy, organization, user, role)
    }
  }

  private[db] def create(implicit c: java.sql.Connection, createdBy: User, organization: Organization, user: User, role: Role): Membership = {
    val membership = Membership(
      guid = UUID.randomUUID,
      organization = organization,
      user = user,
      role = role.key,
      audit = Audit(
        createdAt = DateTime.now,
        createdBy = ReferenceGuid(user.guid),
        updatedAt = DateTime.now,
        updatedBy = ReferenceGuid(user.guid)
      )
    )

    SQL(InsertQuery).on(
      'guid -> membership.guid,
      'organization_guid -> membership.organization.guid,
      'user_guid -> membership.user.guid,
      'role -> membership.role,
      'created_by_guid -> createdBy.guid
    ).execute()

    mainActor ! actors.MainActor.Messages.MembershipCreated(membership.guid)

    membership
  }

  /**
    * Deletes a membership record. Also removes the user from any
    * publication subscriptions that require the administrative role
    * for this org.
    */
  def softDelete(user: User, membership: Membership) {
    subscriptionsDao.deleteSubscriptionsRequiringAdmin(user, membership.organization, membership.user)
    SoftDelete.delete("memberships", user, membership.guid)
  }

  def isUserAdmin(
    user: User,
    organization: Organization
  ): Boolean = {
    findByOrganizationAndUserAndRole(Authorization.All, organization, user, Role.Admin) match {
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
      limit = 1
    ).headOption match {
      case None => false
      case Some(_) => true
    }
  }

  def findByOrganizationAndUserAndRole(
    authorization: Authorization,
    organization: Organization,
    user: User,
    role: Role
  ): Option[Membership] = {
    findAll(authorization, organizationGuid = Some(organization.guid), userGuid = Some(user.guid), role = Some(role.key)).headOption
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[Membership] = {
    findAll(authorization, guid = Some(guid), limit = 1).headOption
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
  ): Seq[Membership] = {
    // TODO Implement authorization

    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and memberships.guid = {guid}::uuid" },
      organizationGuid.map { v => "and memberships.organization_guid = {organization_guid}::uuid" },
      organizationKey.map { v => "and memberships.organization_guid = (select guid from organizations where deleted_at is null and key = {organization_key})" },
      userGuid.map { v => "and memberships.user_guid = {user_guid}::uuid" },
      role.map { v => "and role = {role}" },
      isDeleted.map(Filters.isDeleted("memberships", _)),
      Some(s"order by lower(users.name), lower(users.email) limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString ),
      organizationGuid.map('organization_guid -> _.toString ),
      organizationKey.map('organization_key -> _ ),
      userGuid.map('user_guid -> _.toString),
      role.map('role -> _)
    ).flatten

    DB.withConnection { implicit c =>
      sys.error("TODO PARSER")
      /*
SQL(sql).on(bind: _*)().toList.map { row =>
        Membership(
          guid = row[UUID]("guid"),
          organization = organizationsDao.summaryFromRow(row, Some("organization")),
          user = usersDao.fromRow(row, Some("user")),
          role = row[String]("role"),
          audit = AuditsDao.fromRowCreation(row)
        )
      }.toSeq
       */
    }
  }

}
