package controllers

import io.apibuilder.api.v0.models.{ Membership, Organization, User }
import models.MainTemplate
import lib.Role
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
  sessionId: String,
  user: User,
  request: Request[A]
) extends AuthenticatedRequest[A](sessionId, user, request) {
  require(
    !isAdmin || (isAdmin && isMember),
    "A user that is an admin should always be considered a member"
  )

  override def mainTemplate(title: Option[String] = None): MainTemplate = {
    MainTemplate(
      requestPath = request.path,
      title = title,
      user = Some(user),
      org = Some(org),
      isOrgMember = isMember,
      isOrgAdmin = isAdmin
    )
  }

  def requireAdmin() {
    require(isAdmin, s"Action requires admin role. User[${user.guid}] is not an admin of Org[${org.key}]")
  }

  def requireMember() {
    require(isMember, s"Action requires member role. User[${user.guid}] is not a member of Org[${org.key}]")
  }

}

object AuthenticatedOrg extends ActionBuilder[AuthenticatedOrgRequest] {

  import scala.concurrent.ExecutionContext.Implicits.global

  def invokeBlock[A](request: Request[A], block: (AuthenticatedOrgRequest[A]) => Future[Result]) = {

    def returnUrl(orgKey: Option[String]): Option[String] = {
      if (request.method.toUpperCase == "GET") {
        Some(request.uri)
      } else {
        orgKey match {
          case None => Some(routes.ApplicationController.index().url)
          case Some(key) => Some(routes.Organizations.show(key).url)
        }
      }
    }

    val orgKeyOption = request.path.split("/").drop(1).headOption

    request.session.get("session_id").map { sessionId =>

      lib.ApiClient.awaitCallWith404(Authenticated.api().authentications.getSessionById(sessionId)) match {

        case None => {
          // have a user guid, but user does not exist
          Future.successful(Redirect(routes.LoginController.index(return_url = returnUrl(orgKeyOption))).withNewSession)
        }

        case Some(auth) => {
          val u = auth.user
          val orgKey = orgKeyOption.getOrElse {
            sys.error(s"No org key for request path[${request.path}]")
          }

          val orgOption = lib.ApiClient.awaitCallWith404(Authenticated.api(Some(sessionId)).Organizations.getByKey(orgKey)).headOption
          val memberships = Await.result(Authenticated.api(Some(sessionId)).Memberships.get(orgKey = Some(orgKey), userGuid = Some(u.guid)), 1000.millis)
          orgOption match {
            case None => {
              Future.successful(Redirect("/").flashing("warning" -> s"Organization $orgKey not found"))
            }

            case Some(org: Organization) => {
              val isAdmin = !memberships.find(_.role == Role.Admin.key).isEmpty
              val isMember = isAdmin || !memberships.find(_.role == Role.Member.key).isEmpty
              val authRequest = new AuthenticatedOrgRequest(org, isMember, isAdmin, sessionId, u, request)
              block(authRequest)
            }
          }
        }

      }

    } getOrElse {
      Future.successful(Redirect(routes.LoginController.index(return_url = returnUrl(orgKeyOption))).withNewSession)
    }

  }
}
