package controllers

import db.{ User, UserDao }
import play.api.mvc._
import play.api.mvc.Results.Unauthorized
import scala.concurrent.Future
import util.BasicAuthorization

class AuthenticatedRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

object Authenticated extends ActionBuilder[AuthenticatedRequest] {

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {

    BasicAuthorization.get(request.headers.get("Authorization")) match {

      case Some(auth: BasicAuthorization.Token) => {
        UserDao.findByToken(auth.token) match {

          case Some(u: User) => block(new AuthenticatedRequest(u, request))

          case None => Future.successful(Unauthorized("Invalid token"))

        }
      }

      case _ => Future.successful(Unauthorized("Missing authorization token"))

    }

  }

}
