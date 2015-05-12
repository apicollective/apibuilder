package db

import com.gilt.apidoc.api.v0.models.User
import lib.TokenGenerator
import anorm._
import AnormHelper._
import play.api.db._
import play.api.Play.current
import java.util.UUID
import org.joda.time.DateTime

private[db] case class EmailVerificationConfirmation(
  guid: UUID,
  emailVerificationGuid: UUID,
  createdAt: DateTime
)

private[db] object EmailVerificationConfirmationsDao {

  private val TokenLength = 80
  private val HoursUntilTokenExpires = 168

  private val BaseQuery = """
    select email_verification_confirmations.guid,
           email_verification_confirmations.email_verification_guid,
           email_verification_confirmations.created_at
      from email_verification_confirmations
     where true
  """

  private val InsertQuery = """
    insert into email_verification_confirmations
    (guid, email_verification_guid, created_by_guid)
    values
    ({guid}::uuid, {email_verification_guid}::uuid, {created_by_guid}::uuid)
  """

  def upsert(createdBy: User, conf: EmailVerification): EmailVerificationConfirmation = {
    findAll(emailVerificationGuid = Some(conf.guid), limit = 1).headOption.getOrElse {
      val guid = UUID.randomUUID
      DB.withConnection { implicit c =>
        SQL(InsertQuery).on(
          'guid -> guid,
          'email_verification_guid -> conf.guid,
          'created_by_guid -> createdBy.guid
        ).execute()
      }

      findByGuid(guid).getOrElse {
        sys.error("Failed to create email verification")
      }
    }
  }

  def findByGuid(guid: UUID): Option[EmailVerificationConfirmation] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  private[db] def findAll(
    guid: Option[UUID] = None,
    emailVerificationGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[EmailVerificationConfirmation] = {

    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and email_verification_confirmations.guid = {guid}::uuid" },
      emailVerificationGuid.map { v => "and email_verification_confirmations.email_verification_guid = {email_verification_guid}::uuid" },
      isDeleted.map(Filters.isDeleted("email_verification_confirmations", _)),
      Some(s"order by email_verification_confirmations.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      emailVerificationGuid.map('email_verification_guid -> _.toString)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ) = EmailVerificationConfirmation(
    guid = row[UUID]("guid"),
    emailVerificationGuid = row[UUID]("email_verification_guid"),
    createdAt = row[DateTime]("created_at")
  )

}
