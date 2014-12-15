package db

import com.gilt.apidoc.models.User
import lib.TokenGenerator
import anorm._
import AnormHelper._
import play.api.db._
import play.api.Play.current
import java.util.UUID
import org.joda.time.DateTime

case class EmailVerification(
  guid: UUID,
  userGuid: UUID,
  email: String,
  token: String,
  expiresAt: DateTime
)

object EmailVerificationsDao {

  private val TokenLength = 80
  private val HoursUntilTokenExpires = 168

  private val BaseQuery = """
    select email_verifications.guid,
           email_verifications.user_guid,
           email_verifications.email,
           email_verifications.token,
           email_verifications.expires_at
      from email_verifications
     where email_verifications.deleted_at is null
  """

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
    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'user_guid -> user.guid,
        'email -> email.trim,
        'token -> TokenGenerator.generate(TokenLength),
        'expires_at -> new DateTime().plusHours(HoursUntilTokenExpires),
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create email verification")
    }
  }

  def confirm(user: User, verification: EmailVerification) = {
    EmailVerificationConfirmationsDao.upsert(user, verification)
    // TODO : review pending membership requests based on email domain
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
    limit: Long = 25,
    offset: Long = 0
  ): Seq[EmailVerification] = {

    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and email_verifications.guid = {guid}::uuid" },
      userGuid.map { v => "and email_verifications.user_guid = {user_guid}::uuid" },
      email.map { v => "and lower(email_verifications.email) = lower(trim({email}))" },
      token.map { v => "and email_verifications.token = {token}" },
      isExpired.map { v =>
        v match {
          case true => { "and.email_verifications.expires_at < now()" }
          case false => { "and.email_verifications.expires_at >= now()" }
        }
      },
      Some(s"order by email_verifications.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      userGuid.map('user_guid -> _.toString),
      email.map('email -> _),
      token.map('token -> _)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ) = EmailVerification(
    guid = row[UUID]("guid"),
    userGuid = row[UUID]("user_guid"),
    email = row[String]("email"),
    token = row[String]("token"),
    expiresAt = row[DateTime]("expires_at")
  )

}
