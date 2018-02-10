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
    withOrg(Authorization.User(user.guid), orgKey) { org =>
      withRole(org, user, Role.Member) {
        f(org)
      }
    }
  }

  def withOrgAdmin(user: User, orgKey: String)(f: Organization => Result) = {
    withOrg(Authorization.User(user.guid), orgKey) { org =>
      withRole(org, user, Role.Admin) {
        f(org)
      }
    }
  }

  private[this] def withRole(org: Organization, user: User, role: Role)(f: => Result): Result = {
    membershipsDao.findByOrganizationAndUserAndRole(Authorization.All, org, user, role) match {
      case None => {
        val msg = role match {
          case Role.Admin => s"an admin"
          case Role.Member => "a member"
        }
        Results.Unauthorized(
          Json.toJson(
            Validation.error(
              s"Action requires user to be $msg of the organization"
            )
          )
        )
      }

      case Some(_) => {
        f
      }
    }
  }

}