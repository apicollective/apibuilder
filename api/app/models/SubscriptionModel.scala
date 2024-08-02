package models

import cats.implicits.*
import db.{Authorization, InternalSubscription, InternalUsersDao}
import io.apibuilder.api.v0.models.Subscription
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}

import javax.inject.Inject

class SubscriptionModel @Inject()(
  usersModel: UsersModel,
  orgModel: OrganizationsModel,
) {

  def toModel(mr: InternalSubscription): Option[Subscription] = {
    toModels(Seq(mr)).headOption
  }

  def toModels(subscriptions: Seq[InternalSubscription]): Seq[Subscription] = {
    val users = usersModel.toModelByGuids(subscriptions.map(_.userGuid)).map { u => u.guid -> u }.toMap

    val orgs = orgModel.toModelByGuids(Authorization.All, subscriptions.map(_.organizationGuid))
      .map { o => o.guid -> o }.toMap

    subscriptions.flatMap { s =>
      (users.get(s.userGuid), orgs.get(s.organizationGuid)).mapN { case (user, org) =>
        Subscription(
          guid = s.guid,
          user = user,
          organization = org,
          publication = s.publication,
          audit = Audit(
            createdAt = s.db.createdAt,
            createdBy = ReferenceGuid(s.db.createdByGuid),
            updatedAt = s.db.updatedAt,
            updatedBy = ReferenceGuid(s.db.createdByGuid),
          )
        )
      }
    }
  }
}
