package controllers

import client.{ Apidoc, ApidocClient }
import play.api.mvc._
import play.api.mvc.Results.Redirect
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import play.api.Play.current

class AuthenticatedRequest[A](val user: Apidoc.User, request: Request[A]) extends WrappedRequest[A](request) {

  lazy val client = ApidocClient.instance(user.guid)

  private val apiUrl = current.configuration.getString("apidoc.url").getOrElse {
    sys.error("apidoc.url is required")
  }

  private val apiToken = current.configuration.getString("apidoc.token").getOrElse {
    sys.error("apidoc.token is required")
  }

  // TODO: Rename to api
  lazy val apidocClient = new apidoc.Client(apiUrl, Some(apiToken))
  lazy val generatedClient = apidocClient

}

object Authenticated extends ActionBuilder[AuthenticatedRequest] {

  private lazy val masterClient = ApidocClient.instance("f3973f60-be9f-11e3-b1b6-0800200c9a66")

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {

    request.session.get("user_guid").map { userGuid =>
      val user = Await.result(masterClient.users.findByGuid(userGuid), 5000.millis)
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
