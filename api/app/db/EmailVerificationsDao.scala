package db

import com.bryzek.apidoc.api.v0.models.User
import lib.{Role, TokenGenerator}
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

  private[this] val TokenLength = 80
  private[this] val HoursUntilTokenExpires = 168

  private[this] val BaseQuery = """
    select email_verifications.guid,
           email_verifications.user_guid,
           email_verifications.email,
           email_verifications.token,
           email_verifications.expires_at
      from email_verifications
     where true
  """

  private[this] val InsertQuery = """
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

    global.Actors.mainActor ! actors.MainActor.Messages.EmailVerificationCreated(guid)

    findByGuid(guid).getOrElse {
      sys.error("Failed to create email verification")
    }
  }

  def isExpired(verification: EmailVerification): Boolean = {
    verification.expiresAt.isBeforeNow
  }

  def confirm(user: Option[User], verification: EmailVerification) = {
    assert(
      !EmailVerificationsDao.isExpired(verification),
      "Token for verificationGuid[${verification.guid}] is expired"
    )

    val verificationUser = UsersDao.findByGuid(verification.userGuid).getOrElse {
      sys.error(s"User guid[${verification.userGuid}] does not exist for verification[${verification.guid}]")
    }

    val updatingUser = user.getOrElse(verificationUser)

    EmailVerificationConfirmationsDao.upsert(updatingUser, verification)

    OrganizationsDao.findByEmailDomain(verification.email).foreach { org =>
      MembershipRequestsDao.findByOrganizationAndUserAndRole(Authorization.All, org, verificationUser, Role.Member).map { request =>
        MembershipRequestsDao.acceptViaEmailVerification(updatingUser, request, verification.email)
      }
    }
  }

  def softDelete(deletedBy: User, verification: EmailVerification) {
    SoftDelete.delete("email_verifications", deletedBy, verification.guid)
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
    isDeleted: Option[Boolean] = Some(false),
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
          case true => { "and email_verifications.expires_at < now()" }
          case false => { "and email_verifications.expires_at >= now()" }
        }
      },
      isDeleted.map(Filters.isDeleted("email_verifications", _)),
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
