package db

import io.apibuilder.api.v0.models.User
import lib.TokenGenerator
import anorm._
import anorm.JodaParameterMetaData._
import java.util.UUID
import javax.inject.{Inject, Named, Singleton}

import play.api.db._
import io.flow.postgresql.Query
import org.joda.time.DateTime

case class PasswordReset(
  guid: UUID,
  userGuid: UUID,
  token: String,
  expiresAt: DateTime
)

@Singleton
class PasswordResetRequestsDao @Inject() (
  @Named("main-actor") mainActor: akka.actor.ActorRef,
  @NamedDatabase("default") db: Database,
  userPasswordsDao: UserPasswordsDao,
  usersDao: UsersDao
) {

  private[this] val TokenLength = 80
  private[this] val HoursUntilTokenExpires = 72

  private[this] val dbHelpers = DbHelpers(db, "password_resets")

  private[this] val BaseQuery = Query("""
    select password_resets.guid,
           password_resets.user_guid,
           password_resets.token,
           password_resets.expires_at
      from password_resets
  """)

  private[this] val InsertQuery = """
    insert into password_resets
    (guid, user_guid, token, expires_at, created_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {token}, {expires_at}, {created_by_guid}::uuid)
  """

  def create(createdBy: Option[User], user: User): PasswordReset = {
    val guid = UUID.randomUUID
    db.withConnection { implicit c =>
      SQL(InsertQuery).on(
        "guid" -> guid,
        "user_guid" -> user.guid,
        "token" -> TokenGenerator.generate(TokenLength),
        "expires_at" -> DateTime.now.plusHours(HoursUntilTokenExpires),
        "created_by_guid" -> createdBy.getOrElse(user).guid
      ).execute()
    }

    mainActor ! actors.MainActor.Messages.PasswordResetRequestCreated(guid)

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

  private[this] def parser(): RowParser[PasswordReset] = {
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
