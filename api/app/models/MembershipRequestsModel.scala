package models

import cats.implicits._
import db.{Authorization, InternalMembershipRequest, InternalOrganizationsDao, InternalUsersDao}
import io.apibuilder.api.v0.models.MembershipRequest

import javax.inject.Inject

class MembershipRequestsModel @Inject() (
  orgModel: OrganizationsModel,
  usersModel: UsersModel
) {

  def toModel(mr: InternalMembershipRequest): Option[MembershipRequest] = {
    toModels(Seq(mr)).headOption
  }

  def toModels(requests: Seq[InternalMembershipRequest]): Seq[MembershipRequest] = {
    val users = usersModel.toModelByGuids(requests.map(_.userGuid)).map { u => u.guid -> u }.toMap

    val orgs = orgModel.toModelByGuids(Authorization.All, requests.map(_.organizationGuid))
      .map { o => o.guid -> o }.toMap

    requests.flatMap { r =>
      (users.get(r.userGuid), orgs.get(r.organizationGuid)).mapN { case (user, org) =>
        MembershipRequest(
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