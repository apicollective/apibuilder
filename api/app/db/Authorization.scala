package db

import java.util.UUID

sealed trait Authorization

case object Authorization {

  case object PublicOnly extends Authorization
  private[db] case object All extends Authorization
  case class User(userGuid: UUID) extends Authorization

}
