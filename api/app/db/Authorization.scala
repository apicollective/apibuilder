package db

import java.util.UUID

sealed trait Authorization

case object Authorization {

  case object PublicOnly extends Authorization
  private[db] case object All extends Authorization
  case class User(userGuid: UUID) extends Authorization

  def apply(user: Option[com.gilt.apidoc.models.User]): Authorization = {
    user match {
      case None => PublicOnly
      case Some(u) => User(u.guid)
    }
  }

}
