package controllers

import com.gilt.apidoc.api.v0.models.{Organization, User}
import db.{MembershipsDao, UsersDao}
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


private[controllers] object RequestHelper {

  val UserGuidHeader = "X-User-Guid"
  val AuthorizationHeader = "Authorization"

  def userAuth(
    authorizationHeader: Option[String],
    userGuidHeader: Option[String]
  ): UserAuth = {

    val tokenUser: Option[User] = BasicAuthorization.get(authorizationHeader) match {
      case Some(auth: BasicAuthorization.Token) => {
        UsersDao.findByToken(auth.token)
      }
      case _ => None
    }

    userGuidHeader.flatMap(UsersDao.findByGuid(_)) match {
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
            if (u.email == UsersDao.AdminUserEmail) {
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

class AnonymousRequest[A](val tokenUser: Option[User], val user: Option[User], request: Request[A]) extends WrappedRequest[A](request)

object AnonymousRequest extends ActionBuilder[AnonymousRequest] {

  def invokeBlock[A](request: Request[A], block: (AnonymousRequest[A]) => Future[Result]) = {
    val userAuth = RequestHelper.userAuth(
      request.headers.get(RequestHelper.AuthorizationHeader),
      request.headers.get(RequestHelper.UserGuidHeader)
    )

    block(
      new AnonymousRequest(
        tokenUser = userAuth.tokenUser,
        user = userAuth.user,
        request = request
      )
    )
  }

}

class AuthenticatedRequest[A](val tokenUser: User, val user: User, request: Request[A]) extends WrappedRequest[A](request) {

  def requireAdmin(org: Organization) {
    require(MembershipsDao.isUserAdmin(user, org), s"Action requires admin role. User[${user.guid}] is not an admin of Org[${org.key}]")
  }

  def requireMember(org: Organization) {
    require(MembershipsDao.isUserMember(user, org), s"Action requires member role. User[${user.guid}] is not a member of Org[${org.key}]")
  }

}

object Authenticated extends ActionBuilder[AuthenticatedRequest] {

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {
    val userAuth = RequestHelper.userAuth(
      request.headers.get(RequestHelper.AuthorizationHeader),
      request.headers.get(RequestHelper.UserGuidHeader)
    )

    userAuth.user match {
      case None => {
        Future.successful(Unauthorized(s"Failed basic authorization or missing ${RequestHelper.UserGuidHeader} header"))
      }

      case Some(user) => {
        block(new AuthenticatedRequest(userAuth.tokenUser.get, user, request))
      }
    }
  }

}
