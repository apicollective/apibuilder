package db

import com.gilt.quality.models.{Error, Publication, Subscription, SubscriptionForm, Team, User}
import anorm._
import lib.Validation
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object SubscriptionDao {

  private val BaseQuery = """
    select subscriptions.id,
           subscriptions.publication,
           users.guid as user_guid,
           users.email as user_email,
           users.name as user_name,
           organizations.guid as organization_guid,
           organizations.key as organization_key,
           organizations.name as organization_name
      from subscriptions
      join users on users.guid = subscriptions.user_guid and users.deleted_at is null
      join organizations on organizations.id = subscriptions.organization_id and organizations.deleted_at is null
     where subscriptions.deleted_at is null
  """

  private val SoftDeleteQuery = """
    update subscriptions
       set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now()
     where deleted_at is null
       and user_guid = {user_guid}::uuid
       and publication = {publication}
  """

  def validate(
    form: SubscriptionForm
  ): Seq[Error] = {
    val organizationKeyErrors = OrganizationsDao.lookupId(form.organizationKey) match {
        case None => Seq("Organization not found")
        case Some(_) => Seq.empty
    }

    val publicationErrors = form.publication match {
      case Publication.UNDEFINED(_) => Seq("Publication not found")
      case _ => Seq.empty
    }

    val userErrors = UsersDao.findByGuid(form.userGuid) match {
        case None => Seq("User not found")
        case Some(_) => Seq.empty
    }

    val alreadySubscribed = SubscriptionDao.findAll(
      organizationKey = Some(form.organizationKey),
      userGuid = Some(form.userGuid),
      publication = Some(form.publication),
      limit = 1
    ).headOption match {
      case None => Seq.empty
      case Some(_) => Seq("User is already subscribed to this publication for this organization")
    }

    Validation.errors(organizationKeyErrors ++ publicationErrors ++ userErrors ++ alreadySubscribed)
  }

  def create(createdBy: User, form: SubscriptionForm): Subscription = {
    val errors = validate(form)
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    val organizationId = OrganizationsDao.lookupId(form.organizationKey).get

    val id = DB.withConnection { implicit c =>
      SQL("""
        insert into subscriptions
        (organization_id, publication, user_guid, created_by_guid)
        values
        ({organization_id}, {publication}, {user_guid}::uuid, {created_by_guid}::uuid)
      """).on(
        'organization_id -> organizationId,
        'publication -> form.publication.toString,
        'user_guid -> form.userGuid,
        'created_by_guid -> createdBy.guid
      ).executeInsert().getOrElse(sys.error("Missing id"))
    }

    findById(id).getOrElse {
      sys.error("Failed to create subscription")
    }
  }

  def softDelete(deletedBy: User, subscription: Subscription) {
    DB.withConnection { implicit c =>
      SQL(SoftDeleteQuery).on(
        'deleted_by_guid -> deletedBy.guid,
        'publication -> subscription.publication.toString,
        'user_guid -> subscription.user.guid
      ).execute()
    }
  }

  def findById(guid: UUID): Option[Subscription] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    organization: Option[Organization] = None,
    userGuid: Option[UUID] = None,
    publication: Option[Publication] = None,
    limit: Long = 50,
    offset: Long = 0
  ): Seq[Subscription] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and subscriptions.guid = {guid}" },
      organization.map { v => "and subscriptions.organization_guid = {organization_guid}" },
      userGuid.map { v => "and subscriptions.user_guid = {user_guid}::uuid" },
      publication.map { v => "and subscriptions.publication = {publication}" },
      Some(s"order by subscriptions.id limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      organization.map('organization_guid -> _.guid.toString),
      userGuid.map('user_guid -> _.toString),
      publication.map('publication -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ): Subscription = {
    Subscription(
      id = row[Long]("id"),
      organization = OrganizationDao.summaryFromRow(row, Some("organization")),
      user = UserDao.fromRow(row, Some("user")),
      publication = Publication(row[String]("publication"))
    )
  }

}
