package db

import anorm.JodaParameterMetaData._
import anorm._
import io.apibuilder.api.v0.models.User
import io.apibuilder.task.v0.models.EmailDataEmailVerificationCreated
import io.flow.postgresql.Query
import lib.TokenGenerator
import org.joda.time.DateTime
import play.api.db._
import processor.EmailProcessorQueue

import java.util.UUID
import javax.inject.{Inject, Singleton}

case class EmailVerification(
  guid: UUID,
  userGuid: UUID,
  email: String,
  token: String,
  expiresAt: DateTime
)

@Singleton
class EmailVerificationsDao @Inject() (
  @NamedDatabase("default") db: Database,
  emailQueue: EmailProcessorQueue,
) {

  private val dbHelpers = DbHelpers(db, "email_verifications")

  private val TokenLength = 80
  private val HoursUntilTokenExpires = 168

  private val BaseQuery = Query("""
    select email_verifications.guid,
           email_verifications.user_guid,
           email_verifications.email,
           email_verifications.token,
           email_verifications.expires_at
      from email_verifications
  """)

  private val InsertQuery = """
    insert into email_verifications
    (guid, user_guid, email, token, expires_at, created_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {email}, {token}, {expires_at}, {created_by_guid}::uuid)
  """

  def upsert(createdBy: User, user: User, email: String): EmailVerification = {
    findAll(userGuid = Some(user.guid), email = Some(email), isExpired = Some(false), limit = 1).headOption.getOrElse {
      create(createdBy, user, email)
    }
  }

  def create(createdBy: User, user: User, email: String): EmailVerification = {
    val guid = UUID.randomUUID
    db.withTransaction { implicit c =>
      SQL(InsertQuery).on(
        "guid" -> guid,
        "user_guid" -> user.guid,
        "email" -> email.trim,
        "token" -> TokenGenerator.generate(TokenLength),
        "expires_at" -> DateTime.now.plusHours(HoursUntilTokenExpires),
        "created_by_guid" -> createdBy.guid
      ).execute()
      emailQueue.queueWithConnection(c, EmailDataEmailVerificationCreated(guid))
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create email verification")
    }
  }

  def softDelete(deletedBy: User, verification: EmailVerification): Unit = {
    dbHelpers.delete(deletedBy.guid, verification.guid)
  }

  def findByGuid(guid: UUID): Option[EmailVerification] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findByToken(token: String): Option[EmailVerification] = {
    findAll(token = Some(token), limit = 1).headOption
  }

  private[db] def findAll(
    guid: Option[UUID] = None,
    userGuid: Option[UUID] = None,
    email: Option[String] = None,
    token: Option[String] = None,
    isExpired: Option[Boolean] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[EmailVerification] = {
    db.withConnection { implicit c =>
      BaseQuery.
        equals("email_verifications.guid", guid).
        equals("email_verifications.user_guid", userGuid).
        equals("lower(email_verifications.email)", email.map(_.toLowerCase)).
        equals("email_verifications.token", token).
        and(isExpired.map(Filters.isExpired("email_verifications", _))).
        and(isDeleted.map(Filters.isDeleted("email_verifications", _))).
        orderBy("email_verifications.created_at").
        limit(limit).
        offset(offset).
        as(parser.*)
    }
  }

  private val parser: RowParser[EmailVerification] = {
    SqlParser.get[UUID]("guid") ~
    SqlParser.get[UUID]("user_guid") ~
    SqlParser.str("email") ~
    SqlParser.str("token") ~
    SqlParser.get[DateTime]("expires_at") map {
      case guid ~ userGuid ~ email ~ token ~ expiresAt => {
        EmailVerification(
          guid = guid,
          userGuid= userGuid,
          email = email,
          token = token,
          expiresAt = expiresAt
        )
      }
    }
  }
  

}
