package db

import lib.Constants
import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

case class User(guid: UUID, email: String, name: Option[String], imageUrl: Option[String])
case class UserQuery(guid: Option[UUID] = None,
                     email: Option[String] = None,
                     token: Option[String] = None,
                     limit: Int = 50,
                     offset: Int = 0)

object User {

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

  private def update(user: User) {
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
    findAll(UserQuery(token = Some(token), limit = 1)).headOption
  }

  def findByEmail(email: String): Option[User] = {
    findAll(UserQuery(email = Some(email), limit = 1)).headOption
  }

  def findByGuid(guid: String): Option[User] = {
    findByGuid(UUID.fromString(guid))
  }

  def findByGuid(guid: UUID): Option[User] = {
    findAll(UserQuery(guid = Some(guid), limit = 1)).headOption
  }

  def findAll(query: UserQuery): Seq[User] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      query.guid.map { v => "and users.guid = {guid}::uuid" },
      query.email.map { v => "and users.email = trim(lower({email}))" },
      query.token.map { v => "and users.guid = (select user_guid from tokens where token = {token} and deleted_at is null)"},
      Some(s"order by lower(users.email) limit ${query.limit} offset ${query.offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq(
      query.guid.map { v => 'guid -> toParameterValue(v) },
      query.email.map { v => 'email -> toParameterValue(v) },
      query.token.map { v => 'token -> toParameterValue(v) }
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        User(guid = UUID.fromString(row[String]("guid")),
             email = row[String]("email"),
             name = row[Option[String]]("name"),
             imageUrl = row[Option[String]]("image_url"))
      }.toSeq
    }
  }

}
