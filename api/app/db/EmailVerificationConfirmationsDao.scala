package db

import anorm._
import io.flow.postgresql.Query
import org.joda.time.DateTime
import play.api.db._

import java.util.UUID
import javax.inject.Inject

private[db] case class EmailVerificationConfirmation(
  guid: UUID,
  emailVerificationGuid: UUID,
  createdAt: DateTime
)

class EmailVerificationConfirmationsDao @Inject() (
  @NamedDatabase("default") db: Database
) {

  private val BaseQuery = Query(
    """
    select email_verification_confirmations.guid,
           email_verification_confirmations.email_verification_guid,
           email_verification_confirmations.created_at
      from email_verification_confirmations
  """)

  private val InsertQuery =
    """
    insert into email_verification_confirmations
    (guid, email_verification_guid, created_by_guid)
    values
    ({guid}::uuid, {email_verification_guid}::uuid, {created_by_guid}::uuid)
  """

  def upsert(createdBy: UUID, conf: EmailVerification): EmailVerificationConfirmation = {
    findAll(emailVerificationGuid = Some(conf.guid), limit = 1).headOption.getOrElse {
      val guid = UUID.randomUUID
      db.withConnection { implicit c =>
        SQL(InsertQuery).on(
          "guid" -> guid,
          "email_verification_guid" -> conf.guid,
          "created_by_guid" -> createdBy
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

  def findAll(
    guid: Option[UUID] = None,
    emailVerificationGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[EmailVerificationConfirmation] = {
    db.withConnection { implicit c =>
      BaseQuery.
        equals("email_verification_confirmations.guid", guid).
        equals("email_verification_confirmations.email_verification_guid", emailVerificationGuid).
        and(isDeleted.map(Filters.isDeleted("email_verification_confirmations", _))).
        orderBy("email_verification_confirmations.created_at").
        limit(limit).
        offset(offset).
        as(parser().*
        )
    }
  }

  private def parser(): RowParser[EmailVerificationConfirmation] = {
    SqlParser.get[UUID]("guid") ~
      SqlParser.get[UUID]("email_verification_guid") ~
      SqlParser.get[DateTime]("created_at") map {
      case guid ~ emailVerificationGuid ~ createdAt => {
        EmailVerificationConfirmation(
          guid = guid,
          emailVerificationGuid = emailVerificationGuid,
          createdAt = createdAt
        )
      }
    }
  }

}

