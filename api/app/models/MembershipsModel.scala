package models

import cats.implicits._
import db.{Authorization, InternalMembership, InternalUsersDao}
import io.apibuilder.api.v0.models.Membership

import javax.inject.Inject

class MembershipsModel @Inject()(
  usersModel: UsersModel,
  orgModel: OrganizationsModel
) {

  def toModel(mr: InternalMembership): Option[Membership] = {
    toModels(Seq(mr)).headOption
  }

  def toModels(s: Seq[InternalMembership]): Seq[Membership] = {
    val users = usersModel.toModelByGuids(s.map(_.userGuid)).map { u => u.guid -> u }.toMap

    val orgs = orgModel.toModelByGuids(Authorization.All, s.map(_.organizationGuid))
      .map { o => o.guid -> o }.toMap

    s.flatMap { r =>
      (users.get(r.userGuid), orgs.get(r.organizationGuid)).mapN { case (user, org) =>
        Membership(
          guid = r.guid,
          user = user,
          organization = org,
          role = r.role,
          audit = r.audit
        )
      }
    }
  }
}