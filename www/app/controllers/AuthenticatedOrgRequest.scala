package controllers

import apidoc.models.{ Membership, Organization, User }
import core.Role
import play.api.mvc._
import play.api.mvc.Results.Redirect
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import play.api.Play.current
import java.util.UUID

class AuthenticatedOrgRequest[A](
  val org: Organization,
  val isAdmin: Boolean,
  user: User,
  request: Request[A]
) extends AuthenticatedRequest[A](user, request)

object AuthenticatedOrg extends ActionBuilder[AuthenticatedOrgRequest] {

  import scala.concurrent.ExecutionContext.Implicits.global

  def invokeBlock[A](request: Request[A], block: (AuthenticatedOrgRequest[A]) => Future[Result]) = {

    request.session.get("user_guid").map { userGuid =>
      Await.result(Authenticated.api.Users.get(guid = Some(UUID.fromString(userGuid))), 5000.millis).entity.headOption match {

        case None => {
          // have a user guid, but user does not exist
          Future.successful(Redirect("/login").withNewSession)
        }

        case Some(u: User) => {
          val orgKey = request.path.split("/").drop(1).headOption.getOrElse {
            sys.error(s"No org key for request path[${request.path}]")
          }

          val orgOption = Await.result(Authenticated.api.Organizations.get(key = Some(orgKey)), 1000.millis).entity.headOption
          val memberships = Await.result(Authenticated.api.Memberships.get(orgKey = Some(orgKey), userGuid = Some(u.guid)), 1000.millis).entity
          orgOption match {
            case None => {
              Future.successful(Redirect("/").flashing("warning" -> s"Organization $orgKey not found"))
            }

            case Some(org: Organization) => {
              if (memberships.isEmpty) {
                Future.successful(Redirect(routes.Organizations.requestMembership(orgKey)))

              } else {
                val isAdmin = !memberships.find(_.role == Role.Admin.key).isEmpty
                val authRequest = new AuthenticatedOrgRequest(org, isAdmin, u, request)
                block(authRequest)
              }
            }
          }
        }

      }

    } getOrElse {
      Future.successful(Redirect("/login"))

    }

  }
}
