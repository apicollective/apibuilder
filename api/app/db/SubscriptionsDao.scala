package db

import com.bryzek.apidoc.api.v0.models.{Error, Organization, Publication, Subscription, SubscriptionForm, User}
import anorm._
import lib.Validation
import javax.inject.{Inject, Named, Singleton}
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object SubscriptionsDao {
  val PublicationsRequiredAdmin = Seq(Publication.MembershipRequestsCreate, Publication.MembershipsCreate)
}

@Singleton
class SubscriptionsDao @Inject() (
  organizationsDao: OrganizationsDao,
  subscriptionsDao: SubscriptionsDao,
  usersDao: UsersDao
) {

  private[this] val BaseQuery = s"""
    select subscriptions.guid,
           subscriptions.publication,
           ${AuditsDao.queryCreation("subscriptions")},
           users.guid as user_guid,
           users.email as user_email,
           users.nickname as user_nickname,
           users.name as user_name,
           ${AuditsDao.queryWithAlias("users", "user")},
           organizations.guid as organization_guid,
           organizations.key as organization_key,
           organizations.name as organization_name,
           organizations.namespace as organization_namespace,
           organizations.visibility as organization_visibility,
           ${AuditsDao.queryWithAlias("organizations", "organization")}
      from subscriptions
      join users on users.guid = subscriptions.user_guid and users.deleted_at is null
      join organizations on organizations.guid = subscriptions.organization_guid and organizations.deleted_at is null
     where true
  """

  private[this] val InsertQuery = """
    insert into subscriptions
    (guid, organization_guid, publication, user_guid, created_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {publication}, {user_guid}::uuid, {created_by_guid}::uuid)
  """

  def validate(
    user: User,
    form: SubscriptionForm
  ): Seq[Error] = {
    val org = organizationsDao.findByKey(Authorization.User(user.guid), form.organizationKey)

    val organizationKeyErrors = org match {
        case None => Seq("Organization not found")
        case Some(_) => Seq.empty
    }

    val publicationErrors = form.publication match {
      case Publication.UNDEFINED(_) => Seq("Publication not found")
      case _ => Seq.empty
    }

    val userErrors = usersDao.findByGuid(form.userGuid) match {
        case None => Seq("User not found")
        case Some(_) => Seq.empty
    }

    val alreadySubscribed = org match {
      case None => Seq.empty
      case Some(o) => {
        subscriptionsDao.findAll(
          Authorization.All,
          organization = Some(o),
          userGuid = Some(form.userGuid),
          publication = Some(form.publication),
          limit = 1
        ).headOption match {
          case None => Seq.empty
          case Some(_) => Seq("User is already subscribed to this publication for this organization")
        }
      }
    }

    Validation.errors(organizationKeyErrors ++ publicationErrors ++ userErrors ++ alreadySubscribed)
  }

  def create(createdBy: User, form: SubscriptionForm): Subscription = {
    val errors = validate(createdBy, form)
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    val org = organizationsDao.findByKey(Authorization.User(createdBy.guid), form.organizationKey).getOrElse {
      sys.error("Failed to validate org for subscription")
    }

    val guid = UUID.randomUUID

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'organization_guid -> org.guid,
        'publication -> form.publication.toString,
        'user_guid -> form.userGuid,
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    findByGuid(Authorization.All, guid).getOrElse {
      sys.error("Failed to create subscription")
    }
  }

  def softDelete(deletedBy: User, subscription: Subscription) {
    SoftDelete.delete("subscriptions", deletedBy, subscription.guid)
  }

  def deleteSubscriptionsRequiringAdmin(deletedBy: User, organization: Organization, user: User) {
    SubscriptionsDao.PublicationsRequiredAdmin.foreach { publication =>
      subscriptionsDao.findAll(
        Authorization.All,
        organization = Some(organization),
        userGuid = Some(user.guid),
        publication = Some(publication)
      ).foreach { subscription =>
        softDelete(user, subscription)
      }
    }
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[Subscription] = {
    findAll(authorization, guid = Some(guid), limit = 1).headOption
  }

  def findByUserAndGuid(user: User, guid: UUID): Option[Subscription] = {
    findAll(Authorization.User(user.guid), guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    organization: Option[Organization] = None,
    organizationKey: Option[String] = None,
    userGuid: Option[UUID] = None,
    publication: Option[Publication] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Subscription] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      authorization.subscriptionFilter().map(v => "and " + v),
      guid.map { v => "and subscriptions.guid = {guid}::uuid" },
      organization.map { v => "and subscriptions.organization_guid = {organization_guid}::uuid" },
      organizationKey.map { v => "and subscriptions.organization_guid = (select guid from organizations where deleted_at is null and key = lower(trim({organization_key})))" },
      userGuid.map { v => "and subscriptions.user_guid = {user_guid}::uuid" },
      publication.map { v => "and subscriptions.publication = {publication}" },
      isDeleted.map(Filters.isDeleted("subscriptions", _)),
      Some(s"order by subscriptions.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      organization.map('organization_guid -> _.guid.toString),
      organizationKey.map('organization_key -> _),
      userGuid.map('user_guid -> _.toString),
      publication.map('publication -> _.toString)
    ).flatten ++ authorization.bindVariables

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ): Subscription = {
    Subscription(
      guid = row[UUID]("guid"),
      organization = organizationsDao.summaryFromRow(row, Some("organization")),
      user = usersDao.fromRow(row, Some("user")),
      publication = Publication(row[String]("publication")),
      audit = AuditsDao.fromRowCreation(row)
    )
  }

}
