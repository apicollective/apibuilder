package models

import cats.implicits.*
import db.{Authorization, InternalMembership, InternalUsersDao}
import io.apibuilder.api.v0.models.Membership
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}

import javax.inject.Inject

class MembershipsModel @Inject()(
  usersModel: UsersModel,
  orgModel: OrganizationsModel
) {

  def toModel(mr: InternalMembership): Option[Membership] = {
    toModels(Seq(mr)).headOption
  }

  def toModels(memberships: Seq[InternalMembership]): Seq[Membership] = {
    val users = usersModel.toModelByGuids(memberships.map(_.userGuid)).map { u => u.guid -> u }.toMap

    val orgs = orgModel.toModelByGuids(Authorization.All, memberships.map(_.organizationGuid))
      .map { o => o.guid -> o }.toMap

    memberships.flatMap { m =>
      (users.get(m.userGuid), orgs.get(m.organizationGuid)).mapN { case (user, org) =>
        Membership(
          guid = m.guid,
          user = user,
          organization = org,
          role = m.role,
          audit = Audit(
            createdAt = m.db.createdAt,
            createdBy = ReferenceGuid(m.db.createdByGuid),
            updatedAt = m.db.createdAt,
            updatedBy = ReferenceGuid(m.db.createdByGuid),
          )
        )
      }
    }
  }
}