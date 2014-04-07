package controllers

import client.Apidoc
import play.api.mvc._
import play.api.mvc.Results.Redirect
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

class AuthenticatedRequest[A](val user: Apidoc.User, request: Request[A]) extends WrappedRequest[A](request)

object Authenticated extends ActionBuilder[AuthenticatedRequest] {

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[SimpleResult]) = {

    request.session.get("user_guid").map { userGuid =>
      val user = Await.result(Apidoc.users.findByGuid(userGuid), 100 millis)
      user match {

        case None => {
          // have a user guid, but user does not exist
          Future.successful(Redirect("/login").withNewSession)
        }

        case Some(u: Apidoc.User) => {
          block(new AuthenticatedRequest(u, request))
        }

      }

    } getOrElse {
      Future.successful(Redirect("/login"))

    }

  }
}
