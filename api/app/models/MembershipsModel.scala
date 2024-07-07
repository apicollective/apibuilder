package models

import cats.implicits._
import db.{Authorization, InternalMembership, OrganizationsDao, UsersDao}
import io.apibuilder.api.v0.models.Membership

import javax.inject.Inject

class MembershipsModel @Inject()(
                                        organizationsDao: OrganizationsDao,
                                        usersDao: UsersDao
                                        ) {
  def toModel(mr: InternalMembership): Option[Membership] = {
    toModels(Seq(mr)).headOption
  }

  def toModels(s: Seq[InternalMembership]): Seq[Membership] = {
    val users = usersDao.findAll(
      guids = Some(s.map(_.userGuid))
    ).map { u => u.guid -> u }.toMap

    val orgs = organizationsDao.findAll(
      Authorization.All,
      guids = Some(s.map(_.organizationGuid)),
      limit = None
    ).map { o => o.guid -> o }.toMap

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