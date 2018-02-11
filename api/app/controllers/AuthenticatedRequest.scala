package controllers

import javax.inject.Inject

import db.UsersDao
import io.apibuilder.api.v0.models.User
import play.api.mvc._
import util.BasicAuthorization

import scala.concurrent.{ExecutionContext, Future}

class AnonymousRequest[A](
  /**
    * The user as identified by the api token
    */
  val tokenUser: Option[User],

  /**
    * The actual user as identified by the session Id
    */
  val user: Option[User],

  request: Request[A]
) extends WrappedRequest[A](request)


class AuthenticationHeaderResolver @Inject() (
  usersDao: UsersDao
) {

  private[this] val AuthorizationHeader = "Authorization"

  def anonymousRequest[A](request: Request[A]): AnonymousRequest[A] = {
    val tokenUser: Option[User] = BasicAuthorization.get(request.headers.get(AuthorizationHeader)).flatMap {
      case BasicAuthorization.Token(t) => usersDao.findByToken(t)
      case BasicAuthorization.Session(id) => usersDao.findBySessionId(id)
      case _: BasicAuthorization.User => None
    }

    tokenUser match {
      case None => {
        new AnonymousRequest(None, None, request)
      }

      case Some(u) => {
        if (UsersDao.AdminUserEmails.contains(u.email)) {
          new AnonymousRequest(Some(u), None, request)
        } else {
          new AnonymousRequest(Some(u), Some(u), request)
        }
      }
    }
  }
}

class AnonymousAction @Inject()(
  authenticationHeaderResolver: AuthenticationHeaderResolver,
  val parser: BodyParsers.Default
)(
  implicit val executionContext: ExecutionContext
) extends ActionBuilder[AnonymousRequest, AnyContent]
  with ActionTransformer[Request, AnonymousRequest]
{


  def transform[A](request: Request[A]) = {
    Future {
      authenticationHeaderResolver.anonymousRequest(request)
    }
  }
}

class AuthenticatedRequest[A](
  val tokenUser: User,
  val user: User,
  request: Request[A]
) extends WrappedRequest[A](request)

class AuthenticatedAction @Inject()(
  authenticationHeaderResolver: AuthenticationHeaderResolver,
  val parser: BodyParsers.Default
)(
  implicit val executionContext: ExecutionContext
) extends ActionBuilder[AuthenticatedRequest, AnyContent]
  with ActionTransformer[Request, AuthenticatedRequest]
{

  type ResultBlock[A] = (AuthenticatedAction) => Future[Result]

  def invokeBlock[A](request: Request[A], block: ResultBlock[A]): Future[Result] = {
    builder.authenticate(request, { authRequest: AuthenticatedRequest[A, User] =>
      block(new AuthMessagesRequest[A](authRequest.user, messagesApi, request))
    })
  }

  def transform[A](request: Request[A]) = {
    Future {
      val anon = authenticationHeaderResolver.anonymousRequest(request)
      (anon.tokenUser, anon.user) match {
        case (Some(tokenUser), Some(user)) => {
          new AuthenticatedRequest(
            tokenUser = tokenUser,
            user = user,
            request = request
          )
        }
        case (_, _) => sys.error("Not authenticated")
      }
    }
  }
}

