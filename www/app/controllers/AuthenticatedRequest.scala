package controllers

import apidoc.models.User
import client.Apidoc
import play.api.mvc._
import play.api.mvc.Results.Redirect
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import play.api.Play.current
import java.util.UUID

class AuthenticatedRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request) {

  private def configValue(name: String): String = {
    current.configuration.getString(name).getOrElse {
      sys.error(s"Configuration parameter named[$name] is required")
    }
  }

  lazy val api = new apidoc.Client(configValue("apidoc.url"), Some(configValue("apidoc.token"))) {
    override def requestHolder(path: String) = {
      super.requestHolder(path).withHeaders("X-User-Guid" -> user.guid.toString)
    }
  }
}

object Authenticated extends ActionBuilder[AuthenticatedRequest] {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val apiUrl = current.configuration.getString("apidoc.url").getOrElse {
    sys.error("apidoc.url is required")
  }

  private val apiToken = current.configuration.getString("apidoc.token").getOrElse {
    sys.error("apidoc.token is required")
  }

  lazy val api = new apidoc.Client(apiUrl, Some(apiToken))

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {

    request.session.get("user_guid").map { userGuid =>
      Await.result(api.Users.get(guid = Some(UUID.fromString(userGuid))), 5000.millis).entity.headOption match {

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
