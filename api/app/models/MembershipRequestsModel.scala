package models

import cats.implicits._
import db.{Authorization, InternalMembershipRequest, InternalOrganizationsDao, UsersDao}
import io.apibuilder.api.v0.models.MembershipRequest

import javax.inject.Inject

class MembershipRequestsModel @Inject() (
                                          organizationsDao: InternalOrganizationsDao,
                                          usersDao: UsersDao
                                        ) {
  def toModel(mr: InternalMembershipRequest): Option[MembershipRequest] = {
    toModels(Seq(mr)).headOption
  }

  def toModels(requests: Seq[InternalMembershipRequest]): Seq[MembershipRequest] = {
    val users = usersDao.findAll(
      guids = Some(requests.map(_.userGuid))
    ).map { u => u.guid -> u }.toMap

    val orgs = organizationsDao.findAll(
      Authorization.All,
      guids = Some(requests.map(_.organizationGuid)),
      limit = None
    ).map { o => o.guid -> o }.toMap

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