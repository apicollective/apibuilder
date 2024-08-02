package db

import db.generated.EmailVerificationConfirmationsDao
import io.flow.postgresql.{OrderBy, Query}
import org.joda.time.DateTime
import play.api.db.*

import java.util.UUID
import javax.inject.Inject

private[db] case class InternalEmailVerificationConfirmation(db: generated.EmailVerificationConfirmation) {
  val guid: UUID = db.guid
  val emailVerificationGuid: UUID = db.emailVerificationGuid
  val createdAt: DateTime = db.createdAt
}

class InternalEmailVerificationConfirmationsDao @Inject()(
  dao: EmailVerificationConfirmationsDao
) {

  def upsert(createdBy: UUID, verification: InternalEmailVerification): InternalEmailVerificationConfirmation = {
    findAll(emailVerificationGuid = Some(verification.guid), limit = Some(1)).headOption.getOrElse {
      val guid = dao.insert(createdBy, generated.EmailVerificationConfirmationForm(
        emailVerificationGuid = verification.guid
      ))

      findByGuid(guid).getOrElse {
        sys.error("Failed to create email verification confirmation")
      }
    }
  }

  def findByGuid(guid: UUID): Option[InternalEmailVerificationConfirmation] = {
    findAll(guid = Some(guid), limit = Some(1)).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    emailVerificationGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[InternalEmailVerificationConfirmation] = {
    dao.findAll(
      guid = guid,
      emailVerificationGuid = emailVerificationGuid,
      limit = limit,
      offset = offset,
      orderBy = Some(OrderBy("created_at"))
    ) { q =>
      q.and(isDeleted.map(Filters.isDeleted("email_verification_confirmations", _)))
    }.map(InternalEmailVerificationConfirmation(_))
  }

}

