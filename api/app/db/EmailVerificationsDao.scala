package db

import io.apibuilder.api.v0.models.User
import io.flow.postgresql.Query
import lib.{Role, TokenGenerator}
import anorm._
import anorm.JodaParameterMetaData._
import javax.inject.{Inject, Named, Singleton}
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

@Singleton
class EmailVerificationsDao @Inject() (
  @Named("main-actor") mainActor: akka.actor.ActorRef,
  emailVerificationConfirmationsDao: EmailVerificationConfirmationsDao,
  membershipRequestsDao: MembershipRequestsDao,
  organizationsDao: OrganizationsDao,
  usersDao: UsersDao
) {

  private[this] val TokenLength = 80
  private[this] val HoursUntilTokenExpires = 168

  private[this] val BaseQuery = Query("""
    select email_verifications.guid,
           email_verifications.user_guid,
           email_verifications.email,
           email_verifications.token,
           email_verifications.expires_at
      from email_verifications
  """)

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
        'expires_at -> DateTime.now.plusHours(HoursUntilTokenExpires),
        'created_by_guid -> createdBy.guid
      ).execute()
    }

    mainActor ! actors.MainActor.Messages.EmailVerificationCreated(guid)

    findByGuid(guid).getOrElse {
      sys.error("Failed to create email verification")
    }
  }

  def isExpired(verification: EmailVerification): Boolean = {
    verification.expiresAt.isBeforeNow
  }

  def confirm(user: Option[User], verification: EmailVerification) = {
    assert(
      !isExpired(verification),
      "Token for verificationGuid[${verification.guid}] is expired"
    )

    val verificationUser = usersDao.findByGuid(verification.userGuid).getOrElse {
      sys.error(s"User guid[${verification.userGuid}] does not exist for verification[${verification.guid}]")
    }

    val updatingUser = user.getOrElse(verificationUser)

    emailVerificationConfirmationsDao.upsert(updatingUser, verification)
    organizationsDao.findByEmailDomain(verification.email).foreach { org =>
      membershipRequestsDao.findByOrganizationAndUserAndRole(Authorization.All, org, verificationUser, Role.Member).map { request =>
        membershipRequestsDao.acceptViaEmailVerification(updatingUser, request, verification.email)
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
    DB.withConnection { implicit c =>
      BaseQuery.
        equals("email_verifications.guid::uuid", guid).
        equals("email_verifications.user_guid::uuid", userGuid).
        equals("email_verifications.email", email).
        equals("email_verifications.token", token).
        and(isExpired.map(Filters.isExpired("email_verifications", _))).
        and(isDeleted.map(Filters.isDeleted("email_verifications", _))).
        orderBy("email_verifications.created_at").
        limit(limit).
        offset(offset).
        anormSql().as(
          parser().*
        )
    }
  }

  private[this] def parser(): RowParser[EmailVerification] = {
    SqlParser.get[UUID]("guid") ~
    SqlParser.get[UUID]("user_guid") ~
    SqlParser.str("email") ~
    SqlParser.str("token") ~
    SqlParser.get[DateTime]("expires_at") map {
      case guid ~ userGuid ~ email ~ token ~ expiresAt => {
        EmailVerification(
          guid = guid,
          userGuid= userGuid,
          email = email,
          token = token,
          expiresAt = expiresAt
        )
      }
    }
  }
  

}
