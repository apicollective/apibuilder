package db

import anorm._
import io.apibuilder.api.v0.models.{CleartextToken, Error, TokenForm, User}
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}
import io.flow.postgresql.Query
import lib.{TokenGenerator, Validation}
import play.api.db._

import java.util.UUID
import javax.inject.{Inject, Singleton}

case class InternalToken(
                   guid: UUID,
                   description: Option[String],
                   userGuid: UUID,
                   audit: Audit
                   ) {
  val maskedToken: String = "XXX-XXX-XXX"
}

@Singleton
class TokensDao @Inject() (
  @NamedDatabase("default") db: Database,
  usersDao: UsersDao
) {

  private val dbHelpers = DbHelpers(db, "tokens")

  private val BaseQuery = Query(s"""
    select tokens.guid,
           tokens.description,
           tokens.user_guid,
           ${AuditsDao.queryCreationDefaultingUpdatedAt("tokens")}
      from tokens
  """)

  private val FindCleartextQuery = Query("select token from tokens")

  private val InsertQuery = """
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

  def create(user: User, form: TokenForm): InternalToken = {
    val errors = validate(user, form)
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    val guid = UUID.randomUUID

    db.withConnection { implicit c =>
      SQL(InsertQuery).on(
        "guid" -> guid,
        "user_guid" -> form.userGuid,
        "description" -> form.description,
        "token" -> TokenGenerator.generate(),
        "created_by_guid" -> user.guid
      ).execute()
    }

    findByGuid(Authorization.All, guid).getOrElse {
      sys.error("Failed to create token")
    }
  }

  def softDelete(deletedBy: User, token: InternalToken): Unit = {
    dbHelpers.delete(deletedBy, token.guid)
  }

  def findByToken(token: String): Option[InternalToken] = {
    findAll(Authorization.All, token = Some(token)).headOption
  }

  def findCleartextByGuid(authorization: Authorization, guid: UUID): Option[CleartextToken] = {
    db.withConnection { implicit c =>
      authorization.
        tokenFilter(FindCleartextQuery).
        isNull("tokens.deleted_at").
        equals("tokens.guid", guid).
        as(SqlParser.str("token").*).
        headOption.
        map(CleartextToken)
    }
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[InternalToken] = {
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
  ): Seq[InternalToken] = {
    db.withConnection { implicit c =>
      authorization.tokenFilter(BaseQuery).
        equals("tokens.guid", guid).
        equals("tokens.user_guid", userGuid).
        equals("tokens.token", token).
        and(isDeleted.map(Filters.isDeleted("tokens", _))).
        orderBy("tokens.created_at").
        limit(limit).
        offset(offset).
        as(parser.*)
    }
  }

  private val parser: RowParser[InternalToken] = {
    import org.joda.time.DateTime

    SqlParser.get[UUID]("guid") ~
      SqlParser.str("description").? ~
      SqlParser.get[DateTime]("created_at") ~
      SqlParser.get[UUID]("created_by_guid") ~
      SqlParser.get[DateTime]("updated_at") ~
      SqlParser.get[UUID]("updated_by_guid") ~
      SqlParser.get[UUID]("user_guid") map {
      case guid ~ description ~ createdAt ~ createdByGuid ~ updatedAt ~ updatedByGuid ~ userGuid => {
        InternalToken(
          guid = guid,
          description = description,
          userGuid = userGuid,
          audit = Audit(
            createdAt = createdAt,
            createdBy = ReferenceGuid(createdByGuid),
            updatedAt = updatedAt,
            updatedBy = ReferenceGuid(updatedByGuid),
          )
        )
      }
    }
  }
}
