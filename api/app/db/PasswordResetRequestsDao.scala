package db

import anorm.JodaParameterMetaData._
import anorm._
import io.apibuilder.api.v0.models.User
import io.apibuilder.task.v0.models.EmailDataPasswordResetRequestCreated
import io.flow.postgresql.Query
import lib.TokenGenerator
import org.joda.time.DateTime
import play.api.db._
import processor.EmailProcessorQueue

import java.util.UUID
import javax.inject.{Inject, Singleton}

case class PasswordReset(
  guid: UUID,
  userGuid: UUID,
  token: String,
  expiresAt: DateTime
)

@Singleton
class PasswordResetRequestsDao @Inject() (
  @NamedDatabase("default") db: Database,
  emailQueue: EmailProcessorQueue,
  userPasswordsDao: UserPasswordsDao,
  usersDao: UsersDao
) {

  private val TokenLength = 80
  private val HoursUntilTokenExpires = 72

  private val dbHelpers = DbHelpers(db, "password_resets")

  private val BaseQuery = Query("""
    select password_resets.guid,
           password_resets.user_guid,
           password_resets.token,
           password_resets.expires_at
      from password_resets
  """)

  private val InsertQuery = """
    insert into password_resets
    (guid, user_guid, token, expires_at, created_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {token}, {expires_at}, {created_by_guid}::uuid)
  """

  def create(createdBy: Option[User], user: User): PasswordReset = {
    val guid = UUID.randomUUID
    db.withTransaction { implicit c =>
      SQL(InsertQuery).on(
        "guid" -> guid,
        "user_guid" -> user.guid,
        "token" -> TokenGenerator.generate(TokenLength),
        "expires_at" -> DateTime.now.plusHours(HoursUntilTokenExpires),
        "created_by_guid" -> createdBy.getOrElse(user).guid
      ).execute()
      emailQueue.queueWithConnection(c, EmailDataPasswordResetRequestCreated(guid))
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create password reset")
    }
  }

  def isExpired(pr: PasswordReset): Boolean = {
    pr.expiresAt.isBeforeNow
  }

  def resetPassword(user: Option[User], pr: PasswordReset, newPassword: String): Unit = {
    assert(
      !isExpired(pr),
      s"Password reset[${pr.guid}] is expired"
    )

    val prUser = usersDao.findByGuid(pr.userGuid).getOrElse {
      sys.error(s"User guid[${pr.userGuid}] does not exist for pr[${pr.guid}]")
    }

    val updatingUser = user.getOrElse(prUser)

    userPasswordsDao.create(updatingUser, prUser.guid, newPassword)
    softDelete(updatingUser, pr)
  }

  def softDelete(deletedBy: User, pr: PasswordReset): Unit =  {
    dbHelpers.delete(deletedBy, pr.guid)
  }

  def findByGuid(guid: UUID): Option[PasswordReset] = {
    findAll(guid = Some(guid), limit = 1).headOption
  }

  def findByToken(token: String): Option[PasswordReset] = {
    findAll(token = Some(token), limit = 1).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    userGuid: Option[UUID] = None,
    token: Option[String] = None,
    isExpired: Option[Boolean] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[PasswordReset] = {
    db.withConnection { implicit c =>
      BaseQuery.
        equals("password_resets.guid", guid).
        equals("password_resets.user_guid", userGuid).
        equals("password_resets.token", token).
        and(isDeleted.map(Filters.isDeleted("password_resets", _))).
        and(isExpired.map(Filters.isExpired("password_resets", _))).
        orderBy("password_resets.created_at").
        limit(limit).
        offset(offset).
        anormSql().as(parser().*)
    }
  }

  private def parser(): RowParser[PasswordReset] = {
    SqlParser.get[UUID]("guid") ~
    SqlParser.get[UUID]("user_guid") ~
    SqlParser.str("token") ~
    SqlParser.get[DateTime]("expires_at") map {
      case guid ~ userGuid ~ token ~ expiresAt => {
        PasswordReset(
          guid = guid,
          userGuid = userGuid,
          token = token,
          expiresAt = expiresAt
        )
      }
    }
  }

}
