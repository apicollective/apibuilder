package models

import db.{ Organization, Membership, User }

case class UserRole(org: Organization, user: User) {

  private lazy val roles = Membership.findAllForOrganizationAndUser(org, user).map(_.role)

  lazy val label = {
    if (roles.contains(Role.Admin.key)) {
      Role.Admin.name
    } else if (roles.contains(Role.Member.key)) {
      Role.Member.name
    } else {
      sys.error("Invalid role for user: " + roles.mkString(" "))
    }
  }

}
