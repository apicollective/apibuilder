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

  def findByGuid(guid: UUID): Option[EmailVerification] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findByToken(token: String): Option[EmailVerification] = {
    findAll(token = Some(token), limit = 1).headOption
  }

  private[db] def findAll(
    guid: Option[UUID] = None,
    token: Option[String] = None,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[EmailVerification] = {

    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and email_verifications.guid = {guid}::uuid" },
      token.map { v => "and email_verifications.token = {token}" },
      Some(s"order by email_verifications.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
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
