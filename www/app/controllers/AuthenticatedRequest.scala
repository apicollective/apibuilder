package controllers

import lib.Config
import models.MainTemplate
import com.gilt.apidoc.v0.models.User
import play.api.mvc._
import play.api.mvc.Results.Redirect
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import play.api.Play.current
import java.util.UUID

class AuthenticatedRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request) {

  lazy val api = Authenticated.api(Some(user))

  def mainTemplate(title: Option[String] = None): MainTemplate = {
    MainTemplate(
      requestPath = request.path,
      title = title,
      user = Some(user)
    )
  }

}

object Authenticated extends ActionBuilder[AuthenticatedRequest] {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val apiHost = Config.requiredString("apidoc.api.host")

  private lazy val apiAuth = com.gilt.apidoc.v0.Authorization.Basic(Config.requiredString("apidoc.api.token"))

  def api(user: Option[User] = None): com.gilt.apidoc.v0.Client = {
    user match {
      case None => new com.gilt.apidoc.v0.Client(apiHost, Some(apiAuth))
      case Some(u) => new com.gilt.apidoc.v0.Client(apiHost, Some(apiAuth)) {
        override def _requestHolder(path: String) = {
          super._requestHolder(path).withHeaders("X-User-Guid" -> u.guid.toString)
        }
      }
    }
  }

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {

    lazy val returnUrl: Option[String] = {
      if (request.method.toUpperCase == "GET") {
        Some(request.uri)
      } else {
        None
      }
    }

    request.session.get("user_guid").map { userGuid =>
      Await.result(api().Users.getByGuid(UUID.fromString(userGuid)), 5000.millis) match {

        case None => {
          // have a user guid, but user does not exist
          Future.successful(Redirect(routes.LoginController.index(return_url = returnUrl)).withNewSession)
        }

        case Some(u: User) => {
          block(new AuthenticatedRequest(u, request))
        }

      }

    } getOrElse {
      Future.successful(Redirect(routes.LoginController.index(return_url = returnUrl)).withNewSession)
    }

  }
}
