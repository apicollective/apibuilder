package util

import org.apache.commons.codec.binary.Base64

object BasicAuthorization {

  sealed trait Authorization
  case class Session(id: String) extends Authorization
  case class Token(token: String) extends Authorization
  case class User(user: String, password: String) extends Authorization

  def get(value: Option[String]): Option[Authorization] = {
    value.flatMap(get)
  }

  /**
   * Parses the actual authorization header value
   */
  def get(value: String): Option[Authorization] = {
    val parts = value.split(" ").toSeq
    if (parts.length == 2 && parts.head == "Basic") {
      val userPassword = new String(Base64.decodeBase64(parts.last.getBytes)).split(":").toSeq

      if (userPassword.length == 1) {
        Some(Token(userPassword.head))

      } else if (userPassword.length == 2) {
        Some(User(userPassword.head, userPassword.last))

      } else {
        None
      }

    } else if (parts.length == 2 && parts.head.toLowerCase == "session") {
      Some(Session(parts.last))

    } else {
      None
    }
  }

}
