package db

import anorm._
import io.apibuilder.api.v0.models.{Error, User, UserForm, UserUpdateForm}
import io.apibuilder.task.v0.models.TaskType
import io.flow.postgresql.Query
import lib.{Constants, Misc, UrlKey, Validation}
import play.api.db._
import play.api.inject.Injector

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object UsersDao {

  private val AdminUserEmails: Seq[String] = Seq("admin@apibuilder.io")

  val AdminUserGuid: UUID = UUID.fromString("f3973f60-be9f-11e3-b1b6-0800200c9a66")

}

@Singleton
class UsersDao @Inject() (
  @NamedDatabase("default") db: Database,
  injector: Injector,
  emailVerificationsDao: EmailVerificationsDao,
  userPasswordsDao: UserPasswordsDao,
  internalTasksDao: InternalTasksDao,
) {

  // TODO: Inject directly - here because of circular references
  private[this] def membershipRequestsDao = injector.instanceOf[MembershipRequestsDao]

  lazy val AdminUser: User = UsersDao.AdminUserEmails.flatMap(findByEmail).headOption.getOrElse {
    sys.error(s"Failed to find background user w/ email[${UsersDao.AdminUserEmails.mkString(", ")}]")
  }

  private[this] val BaseQuery = Query(
    s"""
    select users.guid,
           users.email,
           users.name,
           users.nickname,
           users.avatar_url,
           users.gravatar_id,
           ${AuditsDao.query("users")}
      from users
  """)

  private[this] val InsertQuery =
    """
    insert into users
    (guid, email, name, nickname, avatar_url, gravatar_id, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {email}, {name}, {nickname}, {avatar_url}, {gravatar_id}, {created_by_guid}::uuid, {updated_by_guid}::uuid)
  """

  private[this] val UpdateQuery =
    """
  update users
     set email = {email},
         name = {name},
         nickname = {nickname}
   where guid = {guid}::uuid
  """

  def validateNewUser(form: UserForm): Seq[Error] = {
    validate(
      form = UserUpdateForm(
        email = form.email,
        nickname = form.nickname.getOrElse(generateNickname(form.email)),
        name = form.name
      ),
      password = Some(form.password)
    )
  }

  def validate(
    form: UserUpdateForm,
    password: Option[String] = None,
    existingUser: Option[User] = None
  ): Seq[Error] = {
    val emailErrors = if (Misc.isValidEmail(form.email)) {
      findByEmail(form.email) match {
        case None => Nil
        case Some(u) => {
          if (existingUser.map(_.guid).contains(u.guid)) {
            Nil
          } else {
            Seq("User with this email address already exists")
          }
        }
      }
    } else {
      Seq("Invalid email address")
    }

    val nicknameErrors = UrlKey.validate(form.nickname) match {
      case Nil => {
        findAll(nickname = Some(form.nickname)).headOption match {
          case None => Nil
          case Some(u) => {
            if (existingUser.map(_.guid).contains(u.guid)) {
              Nil
            } else {
              Seq("User with this nickname already exists")
            }
          }
        }
      }
      case errors => {
        errors
      }
    }

    val passwordErrors = password match {
      case None => Nil
      case Some(pwd) => userPasswordsDao.validate(pwd)
    }

    Validation.errors(emailErrors ++ nicknameErrors) ++ passwordErrors
  }

  def update(updatingUser: User, user: User, form: UserUpdateForm): Unit = {
    val errors = validate(form, existingUser = Some(user))
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    db.withConnection { implicit c =>
      SQL(UpdateQuery).on(
        "guid" -> user.guid,
        "email" -> form.email.trim.toLowerCase,
        "name" -> form.name.map(_.trim),
        "nickname" -> form.nickname.trim.toLowerCase,
        "updated_by_guid" -> updatingUser.guid
      ).execute()
    }

    if (user.email.trim.toLowerCase != form.email.trim.toLowerCase) {
      emailVerificationsDao.upsert(updatingUser, user, form.email)
    }

  }

  def createForGithub(
    login: String,
    email: String,
    name: Option[String],
    avatarUrl: Option[String],
    gravatarId: Option[String]
  ): User = {
    val nickname = generateNickname(login)
    val guid = db.withTransaction { implicit c =>
      doInsert(
        nickname = nickname,
        email = email,
        name = name,
        avatarUrl = avatarUrl,
        gravatarId = gravatarId
      )
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create user")
    }
  }

  private[this] def toOptionString(value: Option[String]): Option[String] = {
    value.flatMap(toOptionString)
  }

  private[this] def toOptionString(value: String): Option[String] = {
    Some(value.trim).filter(_.nonEmpty)
  }

  def create(form: UserForm): User = {
    val errors = validateNewUser(form)
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    val nickname = form.nickname.getOrElse(generateNickname(form.email))
    val guid = db.withTransaction { implicit c =>
      val id = doInsert(
        nickname = nickname,
        email = form.email,
        name = form.name,
        avatarUrl = None,
        gravatarId = None
      )

      userPasswordsDao.doCreate(c, id, id, form.password)

      id
    }

    findByGuid(guid).getOrElse {
      sys.error("Failed to create user")
    }
  }

  private[this] def doInsert(
    nickname: String,
    email: String,
    name: Option[String],
    avatarUrl: Option[String],
    gravatarId: Option[String]
  )(implicit c: java.sql.Connection): UUID = {
    val guid = UUID.randomUUID

    SQL(InsertQuery).on(
      "guid" -> guid,
      "email" -> email.trim.toLowerCase,
      "nickname" -> nickname.trim.toLowerCase,
      "name" -> toOptionString(name),
      "avatar_url" -> toOptionString(avatarUrl),
      "gravatar_id" -> toOptionString(gravatarId),
      "created_by_guid" -> Constants.DefaultUserGuid.toString,
      "updated_by_guid" -> Constants.DefaultUserGuid.toString
    ).execute()

    internalTasksDao.queueWithConnection(c, TaskType.UserCreated, guid.toString)

    guid
  }

  def findByToken(token: String): Option[User] = {
    findAll(token = Some(token)).headOption
  }

  def findBySessionId(sessionId: String): Option[User] = {
    findAll(sessionId = Some(sessionId)).headOption
  }

  def findByEmail(email: String): Option[User] = {
    findAll(email = Some(email)).headOption
  }

  def findByGuid(guid: String): Option[User] = {
    Try(UUID.fromString(guid)) match {
      case Success(g) => findByGuid(g)
      case Failure(_) => None
    }
  }

  def findByGuid(guid: UUID): Option[User] = {
    findAll(guid = Some(guid)).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    email: Option[String] = None,
    nickname: Option[String] = None,
    sessionId: Option[String] = None,
    token: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false)
  ): Seq[User] = {
    require(
      guid.isDefined || email.isDefined || token.isDefined || sessionId.isDefined || nickname.isDefined,
      "Must have either a guid, email, token, sessionId, or nickname"
    )

    db.withConnection { implicit c =>
      BaseQuery.
        equals("users.guid", guid).
        and(
          email.map { _ => "users.email = trim(lower({email}))" }
        ).bind("email", email).
        and(
          nickname.map { _ => "users.nickname = trim(lower({nickname}))" }
        ).bind("nickname", nickname).
        and(
          sessionId.map { _ => "users.guid = (select user_guid from sessions where id = {session_id})" }
        ).bind("session_id", sessionId).
        and(
          token.map { _ => "users.guid = (select user_guid from tokens where token = {token} and deleted_at is null)" }
        ).bind("token", token).
        and(isDeleted.map(Filters.isDeleted("users", _))).
        limit(1).
        anormSql().as(
        io.apibuilder.api.v0.anorm.parsers.User.parser().*
      )
    }
  }

  @tailrec
  private[db] final def generateNickname(input: String, iteration: Int = 1): String = {
    assert(iteration < 100, s"Possible infinite loop - input[$input] iteration[$iteration]")

    val prefix = input.trim.split("@").toList match {
      case username :: _ :: Nil => username.toLowerCase.trim
      case _ => input.toLowerCase.trim
    }

    val fullPrefix = iteration match {
      case 1 => UrlKey.generate(prefix)
      case n => UrlKey.generate(s"$prefix-$n")
    }

    findAll(nickname = Some(fullPrefix)).headOption match {
      case None => fullPrefix
      case Some(_) => generateNickname(input, iteration + 1)
    }
  }

}
