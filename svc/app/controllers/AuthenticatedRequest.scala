package controllers

import db.User
import play.api.mvc._
import play.api.mvc.Results.Unauthorized
import scala.concurrent.Future

class AuthenticatedRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

object Authenticated extends ActionBuilder[AuthenticatedRequest] {

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[SimpleResult]) = {
    request.headers.get("X-Auth").map { token =>
      User.findByToken(token) match {

        case None => {
          Future.successful(Unauthorized("Invalid token"))
        }

        case Some(u: User) => {
          block(new AuthenticatedRequest(u, request))
        }

      }

    } getOrElse {
      Future.successful(Unauthorized("Missing token header"))

    }

  }
}
