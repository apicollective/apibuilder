package controllers

import db.User
import play.api.mvc._
import play.api.mvc.Results.Redirect
import scala.concurrent.Future

class AuthenticatedRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

object Authenticated extends ActionBuilder[AuthenticatedRequest] {

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[SimpleResult]) = {

    request.session.get("user_guid").map { user_guid =>
      User.findByGuid(user_guid) match {

        case None => {
          // have a user guid, but user does not exist
          Future.successful(Redirect("/login").withNewSession)
        }

        case Some(u: User) => {
          block(new AuthenticatedRequest(u, request))
        }

      }

    } getOrElse {
      Future.successful(Redirect("/login"))

    }

  }
}
