package controllers

import io.apibuilder.api.v0.models.{Organization, User}
import io.apibuilder.api.v0.models.json._
import db.{Authorization, MembershipsDao, UsersDao}
import play.api.libs.json.Json
import play.api.mvc._
import play.api.mvc.Results.Unauthorized
import scala.concurrent.Future
import util.BasicAuthorization
import lib.Validation

private[controllers] case class UserAuth(

  /**
    * The user as identified by the api token
    */
  tokenUser: Option[User],

  /**
    * The actual user as identified by the session Id
    */
  user: Option[User]
)


private[controllers] object RequestHelper {

  private[this] def usersDao = play.api.Play.current.injector.instanceOf[UsersDao]

  val AuthorizationHeader = "Authorization"

  def userAuth(headers: Headers): UserAuth = {
    val tokenUser: Option[User] = BasicAuthorization.get(headers.get(AuthorizationHeader)).flatMap {
      case BasicAuthorization.Token(t) => usersDao.findByToken(t)
      case BasicAuthorization.Session(id) => usersDao.findBySessionId(id)
      case _: BasicAuthorization.User => None
    }

    tokenUser match {
      case None => UserAuth(None, None)
      case Some(u) => {
        if (UsersDao.AdminUserEmails.contains(u.email)) {
          UserAuth(Some(u), None)
        } else {
          UserAuth(Some(u), Some(u))
        }
      }
    }
  }

}

class AnonymousRequest[A](
  val tokenUser: Option[User],
  val user: Option[User],
  request: Request[A]
) extends WrappedRequest[A](request) {

  val authorization = Authorization(user.map(_.guid))

}

object AnonymousRequest extends ActionBuilder[AnonymousRequest] {

  def invokeBlock[A](request: Request[A], block: (AnonymousRequest[A]) => Future[Result]) = {
    val userAuth = RequestHelper.userAuth(request.headers)

    block(
      new AnonymousRequest(
        tokenUser = userAuth.tokenUser,
        user = userAuth.user,
        request = request
      )
    )
  }

}

class AuthenticatedRequest[A](
  val tokenUser: User,
  val user: User,
  request: Request[A]
) extends WrappedRequest[A](request) {

  val authorization = Authorization.User(user.guid)

}

object Authenticated extends ActionBuilder[AuthenticatedRequest] {

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {
    val userAuth = RequestHelper.userAuth(request.headers)

    userAuth.user match {
      case None => {
        Future.successful(
          Unauthorized(
            Json.toJson(
              Validation.unauthorized(
                "Authorization failed. Verify the 'Authorization' header is provided and contains a valid token"
              )
            )
          )
        )
      }

      case Some(user) => {
        block(new AuthenticatedRequest(
          userAuth.tokenUser.get,
          user,
          request)
        )
      }
    }
  }

}
