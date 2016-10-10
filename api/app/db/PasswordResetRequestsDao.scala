package db

import com.bryzek.apidoc.api.v0.models.User
import lib.TokenGenerator
import anorm._
import anorm.JodaParameterMetaData._
import javax.inject.{Inject, Named, Singleton}
import play.api.db._
import play.api.Play.current
import java.util.UUID
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
  userPasswordsDao: UserPasswordsDao,
  usersDao: UsersDao
) {

  private[this] val TokenLength = 80
  private[this] val HoursUntilTokenExpires = 72

  private[this] val BaseQuery = """
    select password_resets.guid,
           password_resets.user_guid,
           password_resets.token,
           password_resets.expires_at
      from password_resets
     where true
  """

  private[this] val InsertQuery = """
    insert into password_resets
    (guid, user_guid, token, expires_at, created_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {token}, {expires_at}, {created_by_guid}::uuid)
  """

  def create(createdBy: Option[User], user: User): PasswordReset = {
    val guid = UUID.randomUUID
    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'user_guid -> user.guid,
        'token -> TokenGenerator.generate(TokenLength),
        'expires_at -> new DateTime().plusHours(HoursUntilTokenExpires),
        'created_by_guid -> createdBy.getOrElse(user).guid
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

  def resetPassword(user: Option[User], pr: PasswordReset, newPassword: String) = {
    assert(
      !isExpired(pr),
      "Password reset[${pr.guid}] is expired"
    )

    val prUser = usersDao.findByGuid(pr.userGuid).getOrElse {
      sys.error(s"User guid[${pr.userGuid}] does not exist for pr[${pr.guid}]")
    }

    val updatingUser = user.getOrElse(prUser)

    userPasswordsDao.create(updatingUser, prUser.guid, newPassword)
    softDelete(updatingUser, pr)
  }

  def softDelete(deletedBy: User, pr: PasswordReset) {
    SoftDelete.delete("password_resets", deletedBy, pr.guid)
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

    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v => "and password_resets.guid = {guid}::uuid" },
      userGuid.map { v => "and password_resets.user_guid = {user_guid}::uuid" },
      token.map { v => "and password_resets.token = {token}" },
      isExpired.map(Filters.isExpired("password_resets", _)),
      isDeleted.map(Filters.isDeleted("password_resets", _)),
      Some(s"order by password_resets.created_at limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      userGuid.map('user_guid -> _.toString),
      token.map('token -> _)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row
  ) = PasswordReset(
    guid = row[UUID]("guid"),
    userGuid = row[UUID]("user_guid"),
    token = row[String]("token"),
    expiresAt = row[DateTime]("expires_at")
  )

}
