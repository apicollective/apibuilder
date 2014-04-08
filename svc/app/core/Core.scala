package core

import play.api.libs.json._

object User {
  implicit val userWrites = Json.writes[User]
}

case class User(guid: String, email: String, name: Option[String], imageUrl: Option[String])
case class UserQuery(guid: Option[String] = None,
                     email: Option[String] = None,
                     token: Option[String] = None) {

  require(!guid.isEmpty || !email.isEmpty || !token.isEmpty, "User query must have either a guid, email or token")

}


