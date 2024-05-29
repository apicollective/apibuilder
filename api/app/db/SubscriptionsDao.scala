package db

import io.apibuilder.api.v0.models.{Error, Organization, Publication, Subscription, SubscriptionForm, User}
import io.flow.postgresql.Query
import anorm._
import lib.Validation
import javax.inject.{Inject, Singleton}

import play.api.db._
import java.util.UUID

import play.api.inject.Injector

object SubscriptionsDao {
  val PublicationsRequiredAdmin = Seq(Publication.MembershipRequestsCreate, Publication.MembershipsCreate)
}

@Singleton
class SubscriptionsDao @Inject() (
  @NamedDatabase("default") db: Database,
  injector: Injector
) {

  private[this] val dbHelpers = DbHelpers(db, "subscriptions")

  // TODO: resolve cicrular dependency
  private[this] def organizationsDao = injector.instanceOf[OrganizationsDao]
  private[this] def subscriptionsDao = injector.instanceOf[SubscriptionsDao]
  private[this] def usersDao = injector.instanceOf[UsersDao]

  private[this] val BaseQuery = Query(s"""
    select subscriptions.guid,
           subscriptions.publication,
           ${AuditsDao.queryCreationDefaultingUpdatedAt("subscriptions")},
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
           '[]' as organization_domains,
           ${AuditsDao.queryWithAlias("organizations", "organization")}
      from subscriptions
      join users on users.guid = subscriptions.user_guid and users.deleted_at is null
      join organizations on organizations.guid = subscriptions.organization_guid and organizations.deleted_at is null
  """)

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
        case Some(_) => Nil
    }

    val publicationErrors = form.publication match {
      case Publication.UNDEFINED(_) => Seq("Publication not found")
      case _ => Nil
    }

    val userErrors = usersDao.findByGuid(form.userGuid) match {
        case None => Seq("User not found")
        case Some(_) => Nil
    }

    val alreadySubscribed = org match {
      case None => Nil
      case Some(o) => {
        subscriptionsDao.findAll(
          Authorization.All,
          organization = Some(o),
          userGuid = Some(form.userGuid),
          publication = Some(form.publication),
          limit = 1
        ).headOption match {
          case None => Nil
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

    db.withConnection { implicit c =>
      SQL(InsertQuery).on(
        "guid" -> guid,
        "organization_guid" -> org.guid,
        "publication" -> form.publication.toString,
        "user_guid" -> form.userGuid,
        "created_by_guid" -> createdBy.guid
      ).execute()
    }

    findByGuid(Authorization.All, guid).getOrElse {
      sys.error("Failed to create subscription")
    }
  }

  def softDelete(deletedBy: User, subscription: Subscription): Unit = {
    dbHelpers.delete(deletedBy, subscription.guid)
  }

  def deleteSubscriptionsRequiringAdmin(deletedBy: User, organization: Organization, user: User): Unit = {
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
    db.withConnection { implicit c =>
      authorization.subscriptionFilter(BaseQuery).
        equals("subscriptions.guid", guid).
        equals("subscriptions.organization_guid", organization.map(_.guid)).
        equals("organizations.key", organizationKey.map(_.toLowerCase.trim)).
        equals("subscriptions.user_guid", userGuid).
        equals("subscriptions.publication", publication.map(_.toString)).
        and(isDeleted.map(Filters.isDeleted("subscriptions", _))).
        orderBy("subscriptions.created_at").
        limit(limit).
        offset(offset).
        anormSql().as(
          io.apibuilder.api.v0.anorm.parsers.Subscription.parser().*
        )
    }
  }

}
