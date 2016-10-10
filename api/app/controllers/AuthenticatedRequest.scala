package controllers

import com.bryzek.apidoc.api.v0.models.{Organization, User}
import db.{Authorization, MembershipsDao, UsersDao}
import play.api.mvc._
import play.api.mvc.Results.Unauthorized
import scala.concurrent.Future
import util.BasicAuthorization

private[controllers] case class UserAuth(

  /**
    * The user as identified by the api token
    */
  tokenUser: Option[User],

  /**
    * The user as identified by the UserGuidHeader
    */
  user: Option[User]
)


case class AuthHeaders(
  authorization: Option[String],
  userGuid: Option[String]
) {

  def toSeq(): Seq[(String, String)] = {
    Seq(
      authorization.map { v => (RequestHelper.AuthorizationHeader, v) },
      userGuid.map { v => (RequestHelper.UserGuidHeader, v) }
    ).flatten
  }

}

object AuthHeaders {

  def apply(headers: play.api.mvc.Headers): AuthHeaders = {
    AuthHeaders(
      authorization = headers.get(RequestHelper.AuthorizationHeader),
      userGuid = headers.get(RequestHelper.UserGuidHeader)
    )
  }

}

private[controllers] object RequestHelper {

  private[this] def usersDao = play.api.Play.current.injector.instanceOf[UsersDao]

  val UserGuidHeader = "X-User-Guid"
  val AuthorizationHeader = "Authorization"

  def userAuth(authHeaders: AuthHeaders): UserAuth = {
    val tokenUser: Option[User] = BasicAuthorization.get(authHeaders.authorization) match {
      case Some(auth: BasicAuthorization.Token) => {
        usersDao.findByToken(auth.token)
      }
      case _ => None
    }

    authHeaders.userGuid.flatMap(usersDao.findByGuid(_)) match {
      case None => {
        /**
          * Currently a hack. If the token user is NOT our system user
          * that is used by the webapp, then we assume this is a user
          * accessing the API directly and the user is the same as the
          * token user (if set).
          */
        tokenUser match {
          case None => UserAuth(None, None)
          case Some(u) => {
            if (u.email == usersDao.AdminUserEmail) {
              UserAuth(Some(u), None)
            } else {
              UserAuth(Some(u), Some(u))
            }
          }
        }
      }
      case Some(userFromHeader) => {
        UserAuth(tokenUser, Some(userFromHeader))
      }
    }

  }


}

class AnonymousRequest[A](
  val authHeaders: AuthHeaders,
  val tokenUser: Option[User],
  val user: Option[User],
  request: Request[A]
) extends WrappedRequest[A](request) {

  val authorization = Authorization(user.map(_.guid))

}

object AnonymousRequest extends ActionBuilder[AnonymousRequest] {

  def invokeBlock[A](request: Request[A], block: (AnonymousRequest[A]) => Future[Result]) = {
    val headers = AuthHeaders(request.headers)
    val userAuth = RequestHelper.userAuth(headers)

    block(
      new AnonymousRequest(
        authHeaders = headers,
        tokenUser = userAuth.tokenUser,
        user = userAuth.user,
        request = request
      )
    )
  }

}

class AuthenticatedRequest[A](
  val authHeaders: AuthHeaders,
  val tokenUser: User,
  val user: User,
  request: Request[A]
) extends WrappedRequest[A](request) {

  private[this] def membershipsDao = play.api.Play.current.injector.instanceOf[MembershipsDao]

  val authorization = Authorization.User(user.guid)

  def requireAdmin(org: Organization) {
    require(membershipsDao.isUserAdmin(user, org), s"Action requires admin role. User[${user.guid}] is not an admin of Org[${org.key}]")
  }

  def requireMember(org: Organization) {
    require(membershipsDao.isUserMember(user, org), s"Action requires member role. User[${user.guid}] is not a member of Org[${org.key}]")
  }

}

object Authenticated extends ActionBuilder[AuthenticatedRequest] {

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {
    val headers = AuthHeaders(request.headers)
    val userAuth = RequestHelper.userAuth(headers)

    userAuth.user match {
      case None => {
        Future.successful(Unauthorized(s"Failed basic authorization or missing ${RequestHelper.UserGuidHeader} header"))
      }

      case Some(user) => {
        block(new AuthenticatedRequest(
          authHeaders = headers,
          userAuth.tokenUser.get,
          user,
          request)
        )
      }
    }
  }

}
