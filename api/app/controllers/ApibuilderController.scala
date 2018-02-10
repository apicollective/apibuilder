package controllers

import db.{Authorization, MembershipsDao}
import io.apibuilder.api.v0.models.{Organization, User}

trait ApibuilderController {

  def membershipsDao: MembershipsDao

  def authorization(user: User) = Authorization.User(user.guid)

  def requireAdmin(user: User, org: Organization) {
    require(membershipsDao.isUserAdmin(user, org), s"Action requires admin role. User[${user.guid}] is not an admin of Org[${org.key}]")
  }

  def requireMember(user: User, org: Organization) {
    require(membershipsDao.isUserMember(user, org), s"Action requires member role. User[${user.guid}] is not a member of Org[${org.key}]")
  }

}