package controllers

import com.gilt.apidoc.models.{Organization, User}
import db.{Membership, UserDao}
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
        UserDao.findByToken(auth.token)
      }
      case _ => None
    }

    val userFromHeader = userGuidHeader.flatMap(UserDao.findByGuid(_)) match {
      case None => tokenUser
      case Some(user) => Some(user)
    }
    UserAuth(tokenUser, userFromHeader)

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
    require(Membership.isUserAdmin(user, org), s"Action requires admin role. User[${user.guid}] is not an admin of Org[${org.key}]")
  }

  def requireMember(org: Organization) {
    require(Membership.isUserAdmin(user, org), s"Action requires admin role. User[${user.guid}] is not an admin of Org[${org.key}]")
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
