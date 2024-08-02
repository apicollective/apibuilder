package db

import anorm.JodaParameterMetaData.*
import anorm.*
import db.generated.PasswordResetsDao
import cats.implicits.*
import cats.data.ValidatedNec
import io.apibuilder.api.v0.models.{Error, User}
import io.apibuilder.task.v0.models.EmailDataPasswordResetRequestCreated
import io.flow.postgresql.{Query, OrderBy}
import lib.{TokenGenerator, Validation}
import org.joda.time.DateTime
import play.api.db.*
import processor.EmailProcessorQueue

import java.util.UUID
import javax.inject.{Inject, Singleton}

case class InternalPasswordReset(db: generated.PasswordReset) {
  val guid: UUID = db.guid
  val userGuid: UUID = db.userGuid
  val token: String = db.token
  val expiresAt: DateTime = db.expiresAt
}

class InternalPasswordResetsDao @Inject()(
  dao: PasswordResetsDao,
  emailQueue: EmailProcessorQueue,
  userPasswordsDao: InternalUserPasswordsDao,
  usersDao: InternalUsersDao
) {

  private val TokenLength = 80
  private val HoursUntilTokenExpires = 72

  def create(createdBy: Option[InternalUser], user: InternalUser): InternalPasswordReset = {
    val guid = dao.db.withTransaction { implicit c =>
      val guid = dao.insert(c, createdBy.getOrElse(user).guid, generated.PasswordResetForm(
        userGuid = user.guid,
        token = TokenGenerator.generate(TokenLength),
        expiresAt = DateTime.now.plusHours(HoursUntilTokenExpires),
      ))
      emailQueue.queueWithConnection(c, EmailDataPasswordResetRequestCreated(guid))
      guid
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create password reset")
    }
  }

  private def isExpired(pr: InternalPasswordReset): Boolean = {
    pr.expiresAt.isBeforeNow
  }

  def resetPassword(user: Option[InternalUser], pr: InternalPasswordReset, newPassword: String): ValidatedNec[Error, InternalUser] = {
    if (isExpired(pr)) {
      Validation.singleError("Password reset is expired").invalidNec
    } else {
      usersDao.findByGuid(pr.userGuid).toValidNec(Validation.singleError("User not found")).andThen { prUser =>
        val updatingUser = user.getOrElse(prUser)
        userPasswordsDao.create(updatingUser, prUser.guid, newPassword).map { _ =>
          softDelete(updatingUser, pr)
          prUser
        }
      }
    }
  }

  def softDelete(deletedBy: InternalUser, pr: InternalPasswordReset): Unit =  {
    dao.delete(deletedBy.guid, pr.db)
  }

  def findByGuid(guid: UUID): Option[InternalPasswordReset] = {
    findAll(guid = Some(guid), limit = Some(1)).headOption
  }

  def findByToken(token: String): Option[InternalPasswordReset] = {
    findAll(token = Some(token), limit = Some(1)).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    userGuid: Option[UUID] = None,
    token: Option[String] = None,
    isExpired: Option[Boolean] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[InternalPasswordReset] = {
    dao.findAll(
      guid = guid,
      token = token,
      userGuid = userGuid,
      limit = limit,
      offset = offset,
      orderBy = Some(OrderBy("created_at"))
    ) { q =>
      q.and(isDeleted.map(Filters.isDeleted("password_resets", _)))
        .and(isExpired.map(Filters.isExpired("password_resets", _)))
    }.map(InternalPasswordReset(_))
  }

}
