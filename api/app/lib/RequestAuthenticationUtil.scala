package lib

import javax.inject.Inject

import db.UsersDao
import io.apibuilder.api.v0.models.User
import play.api.mvc.Headers
import util.BasicAuthorization

/**
  * Helpers to fetch a user from an incoming request header
  */
class RequestAuthenticationUtil @Inject() (
  usersDao: UsersDao
) {
  val AuthorizationHeader = "Authorization"

  def user(headers: Headers): Option[User] = {
    BasicAuthorization.get(headers.get(AuthorizationHeader)).flatMap {
      case BasicAuthorization.Token(t) => usersDao.findByToken(t)
      case BasicAuthorization.Session(id) => usersDao.findBySessionId(id)
      case _: BasicAuthorization.User => None
    }
  }

}
