package controllers

import com.gilt.apidoc.models.{ Membership, Organization, User }
import models.MainTemplate
import core.Role
import play.api.mvc._
import play.api.mvc.Results.Redirect
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import play.api.Play.current
import java.util.UUID

class AuthenticatedOrgRequest[A](
  val org: Organization,
  val isMember: Boolean,
  val isAdmin: Boolean,
  user: User,
  request: Request[A]
) extends AuthenticatedRequest[A](user, request) {

  def mainTemplate(title: String): MainTemplate = {
    MainTemplate(
      title = s"${org.name}: $title",
      user = Some(user),
      org = Some(org)
    )
  }

}

object AuthenticatedOrg extends ActionBuilder[AuthenticatedOrgRequest] {

  import scala.concurrent.ExecutionContext.Implicits.global

  def invokeBlock[A](request: Request[A], block: (AuthenticatedOrgRequest[A]) => Future[Result]) = {

    request.session.get("user_guid").map { userGuid =>
      Await.result(Authenticated.api().Users.getByGuid(UUID.fromString(userGuid)), 5000.millis) match {

        case None => {
          // have a user guid, but user does not exist
          Future.successful(Redirect("/login").withNewSession)
        }

        case Some(u: User) => {
          val orgKey = request.path.split("/").drop(1).headOption.getOrElse {
            sys.error(s"No org key for request path[${request.path}]")
          }

          val orgOption = Await.result(Authenticated.api(Some(u)).Organizations.get(key = Some(orgKey)), 1000.millis).headOption
          val memberships = Await.result(Authenticated.api(Some(u)).Memberships.get(orgKey = Some(orgKey), userGuid = Some(u.guid)), 1000.millis)
          orgOption match {
            case None => {
              Future.successful(Redirect("/").flashing("warning" -> s"Organization $orgKey not found"))
            }

            case Some(org: Organization) => {
              val isAdmin = !memberships.find(_.role == Role.Admin.key).isEmpty
              val isMember = isAdmin || !memberships.find(_.role == Role.Member.key).isEmpty
              val authRequest = new AuthenticatedOrgRequest(org, isMember, isAdmin, u, request)
              block(authRequest)
            }
          }
        }

      }

    } getOrElse {
      Future.successful(Redirect("/login"))

    }

  }
}
