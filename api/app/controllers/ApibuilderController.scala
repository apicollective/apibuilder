package controllers

import db.{Authorization, MembershipsDao, OrganizationsDao}
import io.apibuilder.api.v0.models.{Organization, User}
import io.apibuilder.api.v0.models.json._
import lib.{Role, Validation}
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}

trait ApibuilderController {

  def membershipsDao: MembershipsDao

  def organizationsDao: OrganizationsDao

  def withOrg(auth: Authorization, orgKey: String)(f: Organization => Result) = {
    organizationsDao.findByKey(auth, orgKey) match {
      case None => Results.NotFound(
        Json.toJson(
          Validation.error(
            s"Organization[$orgKey] does not exist or you are not authorized to access it"
          )
        )
      )

      case Some(org) => {
        f(org)
      }
    }
  }

  def withOrgMember(user: User, orgKey: String)(f: Organization => Result) = {
    println(s"withOrgMember(${user.guid}, $orgKey)")
    withOrg(Authorization.User(user.guid), orgKey) { org =>
      println(s"org[${org.guid}] membership check")
      withRole(org, user, Role.All) {
        println("is a member")
        f(org)
      }
    }
  }

  def withOrgAdmin(user: User, orgKey: String)(f: Organization => Result) = {
    withOrg(Authorization.User(user.guid), orgKey) { org =>
      withRole(org, user, Seq(Role.Admin)) {
        f(org)
      }
    }
  }

  private[this] def withRole(org: Organization, user: User, roles: Seq[Role])(f: => Result): Result = {
    val actualRoles = membershipsDao.findByOrganizationAndUserAndRoles(
      Authorization.All, org, user, roles
    ).map(_.role)

    if (actualRoles.isEmpty) {
      val msg: String = if (roles.contains(Role.Admin)) {
        s"an '${Role.Admin}'"
      } else {
        s"a '${Role.Member}'"
      }
      Results.Unauthorized(
        Json.toJson(
          Validation.error(
            s"Must be $msg of the organization"
          )
        )
      )
    } else {
      f
    }
  }

}