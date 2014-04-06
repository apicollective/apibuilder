package core

import play.api.libs.json._

object User {
  implicit val userReads = Json.reads[User]
  implicit val userWrites = Json.writes[User]
}

case class User(guid: String, email: String, name: Option[String], imageUrl: Option[String])
case class UserQuery(guid: Option[String] = None,
                     email: Option[String] = None,
                     token: Option[String] = None) {

  require(!guid.isEmpty || !email.isEmpty || !token.isEmpty, "User query must have either a guid, email or token")

  lazy val params = Seq("guid" -> guid, "email" -> email, "token" -> token) collect { case (k,Some(v)) => (k -> v) }

}

object Organization {
  implicit val organizationReads = Json.reads[Organization]
  implicit val organizationWrites = Json.writes[Organization]
}

case class Organization(guid: String, name: String, key: String)
case class OrganizationQuery(user_guid: String,
                             guid: Option[String] = None,
                             key: Option[String] = None,
                             limit: Int = 50,
                             offset: Int = 0) {

  lazy val params = Seq("user_guid" -> user_guid, "guid" -> guid, "key" -> key, "limit" -> limit, "offset" -> offset) collect { case (k,Some(v)) => (k -> v) }

}
