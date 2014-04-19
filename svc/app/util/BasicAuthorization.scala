package util

import play.api.mvc.Headers

object BasicAuthorization {

  trait Authorization
  case class Token(token: String) extends Authorization
  case class User(user: String, password: String) extends Authorization

  def get(headers: play.api.mvc.Headers): Option[Authorization] = {
    import org.apache.commons.codec.binary.Base64

    headers.get("Authorization").flatMap { value =>
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

      } else {
        None
      }
    }
  }

}
