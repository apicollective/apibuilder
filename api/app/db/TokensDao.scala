package db

import io.apibuilder.api.v0.models.{CleartextToken, Error, Token, TokenForm, User}
import io.flow.postgresql.Query
import lib.TokenGenerator
import anorm._
import javax.inject.{Inject, Singleton}
import play.api.db._
import play.api.Play.current
import java.util.UUID
import lib.Validation

@Singleton
class TokensDao @Inject() (
  usersDao: UsersDao
) {

  private[this] val BaseQuery = Query(s"""
    select tokens.guid,
           'XXX-XXX-XXX' as masked_token,
           tokens.description,
           ${AuditsDao.queryCreationDefaultingUpdatedAt("tokens")},
           users.guid as user_guid,
           users.email as user_email,
           users.nickname as user_nickname,
           users.name as user_name,
           ${AuditsDao.queryWithAlias("users", "user")}
      from tokens
      join users on users.guid = tokens.user_guid and users.deleted_at is null
  """)

  private[this] val FindCleartextQuery = Query(s"""
    select token from tokens where guid = {guid}::uuid and deleted_at is null
  """)

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
      Nil
    } else {
      Seq("You are not authorized to create a token for this user")
    }

    val userErrors = usersDao.findByGuid(form.userGuid) match {
      case None => Seq("User not found")
      case Some(_) => Nil
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
    DB.withConnection { implicit c =>
      authorization.
        tokenFilter(FindCleartextQuery).
        bind("guid", guid).
        anormSql.as(SqlParser.str("token").*).headOption.map(CleartextToken)
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
    DB.withConnection { implicit c =>
      authorization.tokenFilter(BaseQuery).
        equals("tokens.guid", guid).
        equals("tokens.user_guid", userGuid).
        equals("tokens.token", token).
        and(isDeleted.map(Filters.isDeleted("tokens", _))).
        orderBy("tokens.created_at").
        limit(limit).
        offset(offset).
        anormSql().as(
          io.apibuilder.api.v0.anorm.parsers.Token.parser().*
        )
    }
  }
}
