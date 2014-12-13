package db

import com.gilt.apidoc.models.{Token, TokenForm, User}
import lib.{Constants, Role, TokenGenerator}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

object TokensDao {

  private val BaseQuery = """
    select tokens.guid,
           tokens.token,
           users.guid as user_guid,
           users.email as user_email,
           users.name as user_name
      from tokens
      join users on users.guid = tokens.user_guid and users.deleted_at is null
     where tokens.deleted_at is null
  """

  private val InsertQuery = """
    insert into tokens
    (guid, user_guid, token, description, created_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {token}, {description}, {created_by_guid}::uuid)
  """

  def create(user: User, form: TokenForm): Token = {
    val guid = UUID.randomUUID

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'user_guid -> form.userGuid,
        'description -> form.description,
        'token -> TokenGenerator.generate(),
        'created_by_guid -> user.guid
      ).execute()
    }

    findByGuid(Authorization.All, guid).getOrElse {
      sys.error("Failed to create token")
    }
  }

  def findByToken(token: String): Option[Token] = {
    findAll(Authorization.All, token = Some(token)).headOption
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[Token] = {
    findAll(authorization, guid = Some(guid)).headOption
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    userGuid: Option[UUID] = None,
    token: Option[String] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Token] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and tokens.guid = {guid}::uuid" },
      userGuid.map { v => "and tokens.user_guid = {user_guid}::uuid" },
      token.map { v => "and tokens.token = {token}" },
      Some(s"order by tokens.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      userGuid.map('user_guid -> _.toString),
      token.map('token ->_)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ) = Token(
    guid = row[UUID]("guid"),
    token = row[String]("token"),
    user = UsersDao.fromRow(row, Some("user"))
  )

}
