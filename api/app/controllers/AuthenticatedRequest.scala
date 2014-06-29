package controllers

import db.{ User, UserDao }
import play.api.mvc._
import play.api.mvc.Results.Unauthorized
import scala.concurrent.Future
import util.BasicAuthorization

class AuthenticatedRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

object Authenticated extends ActionBuilder[AuthenticatedRequest] {

  private val UserGuidHeader = "X-User-Guid"

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {

    BasicAuthorization.get(request.headers.get("Authorization")) match {

      case Some(auth: BasicAuthorization.Token) => {
        UserDao.findByToken(auth.token) match {

          case Some(u: User) => {
            // We have a valid token. Now check for a user guid header
            request.headers.get(UserGuidHeader) match {
              case None => {
                block(new AuthenticatedRequest(u, request))
              }

              case Some(guid: String) => {
                UserDao.findByGuid(guid) match {
                  case None => {
                    Future.successful(Unauthorized(s"Invalid $UserGuidHeader[$guid]"))
                  }
                  case Some(userFromHeader: User) => {
                    block(new AuthenticatedRequest(userFromHeader, request))
                  }
                }
              }
            }
          }

          case None => Future.successful(Unauthorized("Invalid token"))
        }
      }

      case _ => Future.successful(Unauthorized("Missing authorization token"))

    }

  }

}
