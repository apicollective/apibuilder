package db

import lib.Constants
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object User {
  implicit val userWrites = Json.writes[User]
}

case class User(guid: String, email: String, name: Option[String], imageUrl: Option[String])

object UserDao {

  private val BaseQuery = """
    select guid::varchar, email, name, image_url
      from users
     where deleted_at is null
  """

  def upsert(email: String,
             name: Option[String] = None,
             imageUrl: Option[String] = None): User = {

    findByEmail(email) match {

      case Some(u: User) => {
        if (u.name != name || u.imageUrl != imageUrl) {
          val newUser = u.copy(name = name, imageUrl = imageUrl)
          update(newUser)
          newUser
        } else {
          u
        }
      }

      case None => {
        create(email, name, imageUrl)
      }
    }
  }

  def update(user: User) {
    DB.withConnection { implicit c =>
      SQL("""
          update users
             set email = {email},
                 name = {name},
                 image_url = {image_url}
           where guid = {guid}
          """).on('guid -> user.guid,
                  'email -> user.email,
                  'name -> user.name,
                  'imageUrl -> user.imageUrl,
                  'updated_by_guid -> Constants.DefaultUserGuid).execute()
    }
  }

  private def create(email: String, name: Option[String], imageUrl: Option[String]): User = {
    val guid = UUID.randomUUID
    DB.withConnection { implicit c =>
      SQL("""
          insert into users
          (guid, email, name, image_url, created_by_guid, updated_by_guid)
          values
          ({guid}::uuid, {email}, {name}, {image_url}, {created_by_guid}::uuid, {updated_by_guid}::uuid)
          """).on('guid -> guid,
                  'email -> email,
                  'name -> name,
                  'image_url -> imageUrl,
                  'created_by_guid -> Constants.DefaultUserGuid,
                  'updated_by_guid -> Constants.DefaultUserGuid).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create user")
    }
  }

  def findByToken(token: String): Option[User] = {
    findAll(token = Some(token)).headOption
  }

  def findByEmail(email: String): Option[User] = {
    findAll(email = Some(email)).headOption
  }

  def findByGuid(guid: String): Option[User] = {
    findAll(guid = Some(guid)).headOption
  }

  def findByGuid(guid: UUID): Option[User] = {
    findByGuid(guid.toString)
  }

  def findAll(guid: Option[String] = None,
              email: Option[String] = None,
              token: Option[String] = None): Seq[User] = {
    require(!guid.isEmpty || !email.isEmpty || !token.isEmpty, "Must have either a guid, email or token")

    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v =>
        try {
          val uuid = UUID.fromString(v)
          "and users.guid = {guid}::uuid"
        } catch {
          // not a valid guid - won't match anything
          case e: IllegalArgumentException => "and false"
        }
      },
      guid.map { v => "and users.guid = {guid}::uuid" },
      email.map { v => "and users.email = trim(lower({email}))" },
      token.map { v => "and users.guid = (select user_guid from tokens where token = {token} and deleted_at is null)"},
      Some("limit 1")
    ).flatten.mkString("\n   ")

    val bind = Seq(
      guid.map { v => 'guid -> toParameterValue(v) },
      email.map { v => 'email -> toParameterValue(v) },
      token.map { v => 'token -> toParameterValue(v) }
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        User(guid = row[String]("guid"),
             email = row[String]("email"),
             name = row[Option[String]]("name"),
             imageUrl = row[Option[String]]("image_url"))
      }.toSeq
    }
  }

}
