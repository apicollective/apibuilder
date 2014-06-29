package controllers

import apidoc.models.{ User, Organization }
import client.{ Apidoc, ApidocClient }
import play.api.mvc._
import play.api.mvc.Results.Redirect
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import play.api.Play.current
import java.util.UUID

class AuthenticatedOrgRequest[A](
  val org: Organization,
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

          Await.result(Authenticated.api.Organizations.get(key = Some(orgKey)), 1000.millis).entity.headOption match {
            case None => {
              Future.successful(Redirect("/").flashing("warning" -> s"Organization $orgKey not found"))
            }

            case Some(org: Organization) => {
              block(new AuthenticatedOrgRequest(org, u, request))
            }
          }
        }

      }

    } getOrElse {
      Future.successful(Redirect("/login"))

    }

  }
}
