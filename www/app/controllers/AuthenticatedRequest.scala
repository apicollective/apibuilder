package controllers

import com.gilt.apidoc.models.User
import play.api.mvc._
import play.api.mvc.Results.Redirect
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import play.api.Play.current
import java.util.UUID

class AnonymousRequest[A](val user: Option[User], request: Request[A]) extends WrappedRequest[A](request) {

  lazy val api = Authenticated.api(user)

}

object AnonymousRequest extends ActionBuilder[AnonymousRequest] {

  import scala.concurrent.ExecutionContext.Implicits.global

  def invokeBlock[A](request: Request[A], block: (AnonymousRequest[A]) => Future[Result]) = {
    val user = request.session.get("user_guid").flatMap { guid =>
      Await.result(Authenticated.api().Users.getByGuid(UUID.fromString(guid)), 5000.millis)
    }

    block(new AnonymousRequest(user, request))
  }

}

class AuthenticatedRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request) {

  lazy val api = Authenticated.api(Some(user))

}

object Authenticated extends ActionBuilder[AuthenticatedRequest] {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val apiUrl = current.configuration.getString("apidoc.url").getOrElse {
    sys.error("apidoc.url is required")
  }

  private val apiToken = current.configuration.getString("apidoc.token").getOrElse {
    sys.error("apidoc.token is required")
  }

  def api(user: Option[User] = None): com.gilt.apidoc.Client = {
    user match {
      case None => new com.gilt.apidoc.Client(apiUrl, Some(apiToken))
      case Some(u) => new com.gilt.apidoc.Client(apiUrl, Some(apiToken)) {
        override def _requestHolder(path: String) = {
          super._requestHolder(path).withHeaders("X-User-Guid" -> u.guid.toString)
        }
      }
    }
  }

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {

    request.session.get("user_guid").map { userGuid =>
      Await.result(api().Users.getByGuid(UUID.fromString(userGuid)), 5000.millis) match {

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
