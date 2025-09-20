package db

import cats.data.ValidatedNec
import cats.implicits.*
import db.generated.{Organization, OrganizationsDao, SubscriptionsDao, UsersDao}
import io.apibuilder.api.v0.models.{Error, Publication, SubscriptionForm, User}
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}
import io.flow.postgresql.{OrderBy, Query}
import lib.Validation
import play.api.db.*
import play.api.inject.Injector
import util.OptionalQueryFilter

import java.util.UUID
import javax.inject.{Inject, Singleton}

case class InternalSubscription(db: generated.Subscription) {
  val guid: UUID = db.guid
  val publication: Publication = Publication(db.publication)
  val organizationGuid: UUID = db.organizationGuid
  val userGuid: UUID = db.userGuid
}

object InternalSubscriptionsDao {
  val PublicationsRequiredAdmin: Seq[Publication] = Seq(Publication.MembershipRequestsCreate, Publication.MembershipsCreate)
}

case class ValidatedSubscriptionForm(
  org: db.generated.Organization,
  user: db.generated.User,
  publication: Publication
)

class InternalSubscriptionsDao @Inject()(
  dao: SubscriptionsDao,
  organizationsDao: OrganizationsDao,
  usersDao: UsersDao,
  injector: Injector
) {

  private def validateOrg(key: String): ValidatedNec[Error, Organization] = {
    organizationsDao.findAll(
      limit = Some(1),
    )( using (q: Query) => {
        q.equals("key", key)
          .isNull("deleted_at")
      })
      .headOption
      .toValidNec(Validation.singleError("Organization not found"))
  }

  private def validatePublication(publication: Publication): ValidatedNec[Error, Unit] = {
    publication match {
      case Publication.UNDEFINED(_) => Validation.singleError("Publication not found").invalidNec
      case _ => ().validNec
    }
  }

  private def validateAlreadySubscribed(org: Organization, form: SubscriptionForm): ValidatedNec[Error, Unit] = {
    findAll(
      Authorization.All,
      organizationGuid = Some(org.guid),
      userGuid = Some(form.userGuid),
      publication = Some(form.publication),
      limit = Some(1)
    ).headOption match {
      case None => ().validNec
      case Some(_) => Validation.singleError("User is already subscribed to this publication for this organization").invalidNec
    }
  }

  private def validate(form: SubscriptionForm): ValidatedNec[Error, ValidatedSubscriptionForm] = {
    (
      validateOrg(form.organizationKey).andThen { org =>
        validateAlreadySubscribed(org, form).map { _ => org}
      },
      usersDao.findByGuid(form.userGuid).toValidNec(Validation.singleError("User not found")),
      validatePublication(form.publication)
    ).mapN { case (org, user, _) =>
      ValidatedSubscriptionForm(org, user, form.publication)
    }
  }

  def create(createdBy: InternalUser, form: SubscriptionForm): ValidatedNec[Error, InternalSubscription] = {
    validate(form).map { vForm =>
      val guid = dao.insert(createdBy.guid, generated.SubscriptionForm(
        organizationGuid = vForm.org.guid,
        publication = vForm.publication.toString,
        userGuid = vForm.user.guid
      ))
      findByGuid(Authorization.All, guid).getOrElse {
        sys.error("Failed to create subscription")
      }
    }
  }

  def softDelete(deletedBy: UserReference, subscription: InternalSubscription): Unit = {
    dao.delete(deletedBy.guid, subscription.db)
  }

  def deleteSubscriptionsRequiringAdmin(deletedBy: UserReference, organizationGuid: UUID, userGuid: UUID): Unit = {
    InternalSubscriptionsDao.PublicationsRequiredAdmin.foreach { publication =>
      findAll(
        Authorization.All,
        organizationGuid = Some(organizationGuid),
        userGuid = Some(userGuid),
        publication = Some(publication),
        limit = None
      ).foreach { subscription =>
        softDelete(deletedBy, subscription)
      }
    }
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[InternalSubscription] = {
    findAll(authorization, guid = Some(guid), limit = Some(1)).headOption
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    organizationGuid: Option[UUID] = None,
    organizationKey: Option[String] = None,
    userGuid: Option[UUID] = None,
    publication: Option[Publication] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[InternalSubscription] = {
    val filters = List(
      new OptionalQueryFilter(organizationKey) {
        override def filter(q: Query, value: String): Query = {
          q.in("organization_guid", Query("select guid from organizations").equals("key", value))
        }
      }
    )

    dao.findAll(
      guid = guid,
      organizationGuid = organizationGuid,
      userGuid = userGuid,
      limit = limit,
      offset = offset,
      orderBy = Some(OrderBy("created_at"))
    )( using (q: Query) => {
      authorization.subscriptionFilter(
        filters.foldLeft(q) { case (q, f) => f.filter(q) },
        "subscriptions"
      )
      .and(isDeleted.map(Filters.isDeleted("subscriptions", _)))
      .equals("publication", publication.map(_.toString))
    }).map(InternalSubscription(_))
  }

}
