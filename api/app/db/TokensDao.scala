package db

import com.bryzek.apidoc.api.v0.models.{CleartextToken, Error, Token, TokenForm, User}
import lib.{Constants, Role, TokenGenerator}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID
import lib.Validation

object TokensDao {

  private[this] val BaseQuery = s"""
    select tokens.guid,
           tokens.token,
           tokens.description,
           ${AuditsDao.queryCreation("tokens")},
           users.guid as user_guid,
           users.email as user_email,
           users.nickname as user_nickname,
           users.name as user_name,
           ${AuditsDao.queryWithAlias("users", "user")}
      from tokens
      join users on users.guid = tokens.user_guid and users.deleted_at is null
     where true
  """

  private[this] val FindCleartextQuery = s"""
    select token from tokens where guid = {guid}::uuid and deleted_at is null
  """

  private[this] val InsertQuery = """
    insert into tokens
    (guid, user_guid, token, description, created_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {token}, {description}, {created_by_guid}::uuid)
  """

  def validate(
    user: User,
    form: TokenForm
  ): Seq[Error] = {
    val authErrors = if (user.guid == form.userGuid) {
      Seq.empty
    } else {
      Seq("You are not authorized to create a token for this user")
    }

    val userErrors = UsersDao.findByGuid(form.userGuid) match {
      case None => Seq("User not found")
      case Some(_) => Seq.empty
    }

    Validation.errors(authErrors ++ userErrors)
  }

  def create(user: User, form: TokenForm): Token = {
    val errors = validate(user, form)
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

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

  def softDelete(deletedBy: User, token: Token) {
    SoftDelete.delete("tokens", deletedBy, token.guid)
  }

  def findByToken(token: String): Option[Token] = {
    findAll(Authorization.All, token = Some(token)).headOption
  }

  def findCleartextByGuid(authorization: Authorization, guid: UUID): Option[CleartextToken] = {
    val sql = Seq(
      Some(FindCleartextQuery.trim),
      authorization.tokenFilter()
    ).flatten.mkString("\n   and ")

    val bind = Seq[NamedParameter]('guid -> guid.toString) ++ authorization.bindVariables
    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row => CleartextToken(row[String]("token")) }.headOption
    }
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[Token] = {
    findAll(authorization, guid = Some(guid)).headOption
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    userGuid: Option[UUID] = None,
    token: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Token] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      authorization.tokenFilter().map(s => s"and $s"),
      guid.map { v => "and tokens.guid = {guid}::uuid" },
      userGuid.map { v => "and tokens.user_guid = {user_guid}::uuid" },
      token.map { v => "and tokens.token = {token}" },
      isDeleted.map(Filters.isDeleted("tokens", _))
    ).flatten.mkString("\n   ") + s" order by tokens.created_at limit ${limit} offset ${offset}"

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      userGuid.map('user_guid -> _.toString),
      token.map('token ->_)
    ).flatten ++ authorization.bindVariables

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ) = Token(
    guid = row[UUID]("guid"),
    maskedToken = obfuscate(row[String]("token")),
    description = row[Option[String]]("description"),
    user = UsersDao.fromRow(row, Some("user")),
    audit = AuditsDao.fromRowCreation(row)
  )

  private[db] def obfuscate(value: String): String = {
    if (value.size >= 15) {
      // 1st 3, mask, + last 4
      val letters = value.split("")
      "XXX-XXXX-" + letters.slice(letters.size-4, letters.size).mkString("")
    } else {
      "XXX-XXXX-XXXX"
    }
  }

}
