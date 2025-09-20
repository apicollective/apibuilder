package db

import db.generated.EmailVerificationsDao
import io.apibuilder.task.v0.models.EmailDataEmailVerificationCreated
import io.flow.postgresql.{OrderBy, Query}
import lib.TokenGenerator
import org.joda.time.DateTime
import processor.EmailProcessorQueue

import java.util.UUID
import javax.inject.Inject

case class InternalEmailVerification(db: generated.EmailVerification) {
  val guid: UUID = db.guid
  val userGuid: UUID = db.userGuid
  val email: String = db.email
  val token: String = db.token
  val expiresAt: DateTime = db.expiresAt
}

class InternalEmailVerificationsDao @Inject()(
  dao: EmailVerificationsDao,
  emailQueue: EmailProcessorQueue,
) {

  private val TokenLength = 80
  private val HoursUntilTokenExpires = 168

  def upsert(createdBy: InternalUser, user: InternalUser, email: String): InternalEmailVerification = {
    findAll(userGuid = Some(user.guid), email = Some(email), isExpired = Some(false), limit = Some(1)).headOption.getOrElse {
      create(createdBy, user, email)
    }
  }

  private[db] def create(createdBy: InternalUser, user: InternalUser, email: String): InternalEmailVerification = {
    val guid = dao.db.withTransaction { c =>
      val guid = dao.insert(c, createdBy.guid, generated.EmailVerificationForm(
        userGuid = user.guid,
        email = email.trim,
        token = TokenGenerator.generate(TokenLength),
        expiresAt = DateTime.now.plusHours(HoursUntilTokenExpires)
      ))
      emailQueue.queueWithConnection(c, EmailDataEmailVerificationCreated(guid))
      guid
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create email verification")
    }
  }

  def softDelete(deletedBy: InternalUser, verification: InternalEmailVerification): Unit = {
    dao.delete(deletedBy.guid, verification.db)
  }

  def findByGuid(guid: UUID): Option[InternalEmailVerification] = {
    findAll(guid = Some(guid), limit = Some(1)).headOption
  }

  def findByToken(token: String): Option[InternalEmailVerification] = {
    findAll(token = Some(token), limit = Some(1)).headOption
  }

  private[db] def findAll(
    guid: Option[UUID] = None,
    userGuid: Option[UUID] = None,
    email: Option[String] = None,
    token: Option[String] = None,
    isExpired: Option[Boolean] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[InternalEmailVerification] = {
    dao.findAll(
      guid = guid,
      token = token,
      userGuid = userGuid,
      limit = limit,
      offset = offset,
      orderBy = Some(OrderBy("created_at"))
    )( using (q: Query) => {
      q.equals("lower(email)", email.map(_.toLowerCase))
        .and(isExpired.map(Filters.isExpired("email_verifications", _)))
        .and(isDeleted.map(Filters.isDeleted("email_verifications", _)))
    }).map(InternalEmailVerification(_))
  }

}
