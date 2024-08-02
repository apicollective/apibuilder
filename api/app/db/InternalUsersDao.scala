package db

import anorm.*
import cats.implicits._
import cats.data.ValidatedNec
import cats.data.Validated.{Invalid, Valid}
import db.generated.UsersDao
import io.apibuilder.api.v0.models.{Error, User, UserForm, UserUpdateForm}
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}
import io.apibuilder.task.v0.models.TaskType
import io.flow.postgresql.Query
import lib.{Constants, Misc, UrlKey, Validation}
import play.api.db.*

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

case class UserReference(guid: UUID)
object UserReference {
  def apply(user: User): UserReference = UserReference(user.guid)
}

case class InternalUser(db: generated.User) {
  val guid: UUID = db.guid
  val name: Option[String] = db.name
  val nickname: String = db.nickname
  val email: String = db.email
  def reference: UserReference = UserReference(guid)
}

case class ValidatedUserForm(
  email: String,
  nickname: String,
  name: Option[String],
  avatarUrl: Option[String],
  gravatarId: Option[String],
  password: Option[ValidatedPassword],
)

class InternalUsersDao @Inject()(
                                  dao: UsersDao,
                                  emailVerificationsDao: InternalEmailVerificationsDao,
                                  userPasswordsDao: InternalUserPasswordsDao,
                                  internalTasksDao: InternalTasksDao,
) {

  lazy val AdminUser: InternalUser = Constants.AdminUserEmails.flatMap(findByEmail).headOption.getOrElse {
    sys.error(s"Failed to find background user w/ email[${Constants.AdminUserEmails.mkString(", ")}]")
  }

  private[db] def validateNewUser(form: UserForm): ValidatedNec[Error, ValidatedUserForm] = {
    validate(
      form = UserUpdateForm(
        email = form.email,
        nickname = form.nickname.getOrElse(generateNickname(form.email)),
        name = form.name
      ),
      password = Some(form.password)
    )
  }

  private def validateEmail(email: String, existingUser: Option[InternalUser]): ValidatedNec[Error, String] = {
    Misc.validateEmail(email.trim.toLowerCase()).andThen { vEmail =>
      findByEmail(vEmail) match {
        case None => vEmail.validNec
        case Some(u) if existingUser.map(_.guid).contains(u.guid) => vEmail.validNec
        case Some(_) => Validation.singleError("User with this email address already exists").invalidNec
      }
    }
  }

  private def validateNickname(nickname: String, existingUser: Option[InternalUser]): ValidatedNec[Error, String] = {
    val trimmed = nickname.trim
    UrlKey.validateNec(trimmed) match {
      case Invalid(e) => Validation.singleError(e.toNonEmptyList.toList.mkString(", ")).invalidNec
      case Valid(_) => {
        findAll(nickname = Some(nickname), limit = Some(1)).headOption match {
          case None => trimmed.validNec
          case Some(u) if existingUser.map(_.guid).contains(u.guid) => trimmed.validNec
          case Some(_) => Validation.singleError("User with this nickname already exists").invalidNec
        }
      }
    }
  }

  private def validatePassword(password: Option[String]): ValidatedNec[Error, Option[ValidatedPassword]] = {
    password match {
      case None => None.validNec
      case Some(pwd) => userPasswordsDao.validate(pwd).map(Some(_))
    }
  }

  private[db] def validate(
    form: UserUpdateForm,
    password: Option[String] = None,
    existingUser: Option[InternalUser] = None
  ): ValidatedNec[Error, ValidatedUserForm] = {
    (
      validateEmail(form.email, existingUser = existingUser),
      validateNickname(form.nickname, existingUser = existingUser),
      validatePassword(password)
    ).mapN { case (vEmail, vNickname, vPassword) =>
      ValidatedUserForm(
        email = vEmail,
        nickname = vNickname,
        name = form.name.map(_.trim).filterNot(_.isEmpty),
        password = vPassword,
        avatarUrl = None,
        gravatarId = None,
      )
    }
  }

  def update(updatingUser: InternalUser, user: InternalUser, form: UserUpdateForm): ValidatedNec[Error, InternalUser] = {
    validate(form, existingUser = Some(user)).map { vForm =>
      dao.update(updatingUser.guid, user.db, user.db.form.copy(
        email = vForm.email,
        name = vForm.name,
        nickname = vForm.nickname,
      ))

      // TODO: Move to inside a transaction
      if (user.email.toLowerCase != vForm.email.toLowerCase) {
        emailVerificationsDao.upsert(updatingUser, user, form.email)
      }

      findByGuid(user.guid).getOrElse {
        sys.error("Failed to update user")
      }
    }
  }

  def createForGithub(
    login: String,
    email: String,
    name: Option[String],
    avatarUrl: Option[String],
    gravatarId: Option[String]
  ): InternalUser = {
    doInsert(Constants.DefaultUserGuid, ValidatedUserForm(
      email = email,
      nickname = generateNickname(login),
      name = name,
      avatarUrl = avatarUrl,
      gravatarId = gravatarId,
      password = None
    ))
  }

  private def toOptionString(value: Option[String]): Option[String] = {
    value.flatMap(toOptionString)
  }

  private def toOptionString(value: String): Option[String] = {
    Some(value.trim).filter(_.nonEmpty)
  }

  def create(form: UserForm): ValidatedNec[Error, InternalUser] = {
    validateNewUser(form).map { vForm =>
      doInsert(Constants.DefaultUserGuid, vForm)
    }
  }

  private def doInsert(createdBy: UUID, form: ValidatedUserForm): InternalUser = {
    val guid = dao.db.withTransaction { implicit c =>
      val userGuid = dao.insert(c, createdBy, generated.UserForm(
        email = form.email,
        nickname = form.nickname,
        name = form.name,
        avatarUrl = form.avatarUrl,
        gravatarId = form.gravatarId
      ))

      form.password.foreach { pwd =>
        userPasswordsDao.doCreate(createdBy, userGuid, pwd)(c)
      }

      internalTasksDao.queueWithConnection(c, TaskType.UserCreated, userGuid.toString)

      userGuid
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create user")
    }
  }

  def findByToken(token: String): Option[InternalUser] = {
    findAll(token = Some(token), limit = Some(1)).headOption
  }

  def findBySessionId(sessionId: String): Option[InternalUser] = {
    findAll(sessionId = Some(sessionId), limit = Some(1)).headOption
  }

  def findByEmail(email: String): Option[InternalUser] = {
    findAll(email = Some(email), limit = Some(1)).headOption
  }

  def findByGuid(guid: String): Option[InternalUser] = {
    Try(UUID.fromString(guid)) match {
      case Success(g) => findByGuid(g)
      case Failure(_) => None
    }
  }

  def findByGuid(guid: UUID): Option[InternalUser] = {
    findAll(guid = Some(guid), limit = Some(1)).headOption
  }

  def findAllByGuids(guids: Seq[UUID]): Seq[InternalUser] = {
    findAll(guids = Some(guids), limit = None)
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    email: Option[String] = None,
    nickname: Option[String] = None,
    sessionId: Option[String] = None,
    token: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long],
  ): Seq[InternalUser] = {
    require(
      guid.isDefined || guids.isDefined || email.isDefined || token.isDefined || sessionId.isDefined || nickname.isDefined,
      "Must have either a guid, email, token, sessionId, or nickname"
    )

    dao.findAll(
      guid = guid,
      guids = guids,
      limit = limit,
    ) { q =>
      q.and(
        email.map { _ => "email = trim(lower({email}))" }
      ).bind("email", email)
      .and(
        nickname.map { _ => "nickname = trim(lower({nickname}))" }
      ).bind("nickname", nickname)
      .and(
        sessionId.map { _ => "guid = (select user_guid from sessions where id = {session_id})" }
      ).bind("session_id", sessionId)
      .and(
        token.map { _ => "guid = (select user_guid from tokens where token = {token} and deleted_at is null)" }
      ).bind("token", token)
      .and(isDeleted.map(Filters.isDeleted("users", _)))
    }.map(InternalUser(_))
  }

  @tailrec
  private[db] final def generateNickname(input: String, iteration: Int = 1): String = {
    assert(iteration < 100, s"Possible infinite loop in generateNickname - input[$input] iteration[$iteration]")

    val prefix = input.trim.split("@").toList match {
      case username :: _ :: Nil => username.toLowerCase.trim
      case _ => input.toLowerCase.trim
    }

    val fullPrefix = iteration match {
      case 1 => UrlKey.generate(prefix)
      case n => UrlKey.generate(s"$prefix-$n")
    }

    findAll(nickname = Some(fullPrefix), limit = Some(1)).headOption match {
      case None => fullPrefix
      case Some(_) => generateNickname(input, iteration + 1)
    }
  }

}
