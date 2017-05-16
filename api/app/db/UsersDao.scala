package db

import com.bryzek.apidoc.api.v0.models.{Error, User, UserForm, UserUpdateForm}
import lib.{Constants, Misc, Role, UrlKey, Validation}
import anorm._
import javax.inject.{Inject, Named, Singleton}
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object UsersDao {

  val AdminUserEmail = "admin@apidoc.me"

}

@Singleton
class UsersDao @Inject() (
  @Named("main-actor") mainActor: akka.actor.ActorRef
) {

  // TODO: Inject directly - here because of circular references
  private[this] def emailVerificationsDao = play.api.Play.current.injector.instanceOf[EmailVerificationsDao]
  private[this] def membershipRequestsDao = play.api.Play.current.injector.instanceOf[MembershipRequestsDao]
  private[this] def organizationsDao = play.api.Play.current.injector.instanceOf[OrganizationsDao]
  private[this] def userPasswordsDao = play.api.Play.current.injector.instanceOf[UserPasswordsDao]

  lazy val AdminUser = findByEmail(UsersDao.AdminUserEmail).getOrElse {
    sys.error(s"Failed to find background user w/ email[${UsersDao.AdminUserEmail}]")
  }

  private[this] val BaseQuery = s"""
    select users.guid,
           users.email,
           users.name,
           users.nickname,
           users.avatar_url,
           users.gravatar_id,
           ${AuditsDao.query("users")}
      from users
     where true
  """

  private[this] val InsertQuery = """
    insert into users
    (guid, email, name, nickname, avatar_url, gravatar_id, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {email}, {name}, {nickname}, {avatar_url}, {gravatar_id}, {created_by_guid}::uuid, {updated_by_guid}::uuid)
  """

  private[this] val UpdateQuery = """
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
    val emailErrors = Misc.isValidEmail(form.email) match {
      case false => Seq("Invalid email address")
      case true => {
        findByEmail(form.email) match {
          case None => Seq.empty
          case Some(u) => {
            if (existingUser.map(_.guid) == Some(u.guid)) {
              Seq.empty
            } else {
              Seq("User with this email address already exists")
            }
          }
        }
      }
    }

    val nicknameErrors = UrlKey.validate(form.nickname) match {
      case Nil => {
        findAll(nickname = Some(form.nickname)).headOption match {
          case None => Seq.empty
          case Some(u) => {
            if (existingUser.map(_.guid) == Some(u.guid)) {
              Seq.empty
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
      case None => Seq.empty
      case Some(pwd) => userPasswordsDao.validate(pwd)
    }

    Validation.errors(emailErrors ++ nicknameErrors) ++ passwordErrors
  }

  def update(updatingUser: User, user: User, form: UserUpdateForm) {
    val errors = validate(form, existingUser = Some(user))
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    DB.withConnection { implicit c =>
      SQL(UpdateQuery).on(
        'guid -> user.guid,
        'email -> form.email.trim.toLowerCase,
        'name -> form.name.map(_.trim),
        'nickname -> form.nickname.trim.toLowerCase,
        'updated_by_guid -> updatingUser.guid
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
    val guid = DB.withConnection { implicit c =>
      doInsert(
        nickname = login,
        email = email,
        name = name,
        avatarUrl = avatarUrl,
        gravatarId = gravatarId
      )
    }

    mainActor ! actors.MainActor.Messages.UserCreated(guid)

    findByGuid(guid).getOrElse {
      sys.error("Failed to create user")
    }
  }

  private[this] def toOptionString(value: Option[String]): Option[String] = {
    value.flatMap(toOptionString)
  }

  private[this] def toOptionString(value: String): Option[String] = {
    value.trim match {
      case "" => None
      case v => Some(v)
    }
  }

  def create(form: UserForm): User = {
    val errors = validateNewUser(form)
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    val guid = DB.withTransaction { implicit c =>
      val id = doInsert(
        nickname = form.nickname.getOrElse(generateNickname(form.email)),
        email = form.email,
        name = form.name,
        avatarUrl = None,
        gravatarId = None
      )

      userPasswordsDao.doCreate(c, id, id, form.password)

      id
    }

    mainActor ! actors.MainActor.Messages.UserCreated(guid)

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
      'guid -> guid,
      'email -> email.trim.toLowerCase,
      'nickname -> nickname.trim.toLowerCase,
      'name -> toOptionString(name),
      'avatar_url -> toOptionString(avatarUrl),
      'gravatar_id -> toOptionString(gravatarId),
      'created_by_guid -> Constants.DefaultUserGuid,
      'updated_by_guid -> Constants.DefaultUserGuid
    ).execute()

    guid
  }

  def processUserCreated(guid: UUID) {
    findByGuid(guid).foreach { user =>
      organizationsDao.findByEmailDomain(user.email).foreach { org =>
        membershipRequestsDao.upsert(user, org, user, Role.Member)
      }
      emailVerificationsDao.create(user, user, user.email)
    }
  }

  def findByToken(token: String): Option[User] = {
    findAll(token = Some(token)).headOption
  }

  def findByEmail(email: String): Option[User] = {
    findAll(email = Some(email)).headOption
  }

  def findByGuid(guid: String): Option[User] = {
    findAll(guid = Some(guid)).headOption
  }

  def findByGuid(guid: UUID): Option[User] = {
    findByGuid(guid.toString)
  }

  def findAll(
    guid: Option[String] = None,
    email: Option[String] = None,
    nickname: Option[String] = None,
    token: Option[String] = None,
    isDeleted: Option[Boolean] = None
  ): Seq[User] = {
    require(!guid.isEmpty || !email.isEmpty || !token.isEmpty || !nickname.isEmpty, "Must have either a guid, email, token, or nickname")

    val sql = Seq(
      Some(BaseQuery.trim),
      guid.map { v =>
        Try(UUID.fromString(v)) match {
          case Success(uuid) => "and users.guid = {guid}::uuid"
          case Failure(e) => e match {
            case e: IllegalArgumentException => "and false"
          }
        }
      },
      guid.map { v => "and users.guid = {guid}::uuid" },
      email.map { v => "and users.email = trim(lower({email}))" },
      nickname.map { v => "and users.nickname = trim(lower({nickname}))" },
      token.map { v => "and users.guid = (select user_guid from tokens where token = {token} and deleted_at is null)"},
      isDeleted.map(Filters.isDeleted("users", _)),
      Some("limit 1")
    ).flatten.mkString("\n   ")

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _),
      email.map('email -> _),
      nickname.map('nickname -> _),
      token.map('token ->_)
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  @tailrec
  private[db] final def generateNickname(input: String, iteration: Int = 1): String = {
    assert(iteration < 100, s"Possible infinite loop - input[$input] iteration[$iteration]")

    val prefix = input.trim.split("@").toList match {
      case username :: domain :: Nil => {
        username.toLowerCase.trim
      }
      case _ => input.toLowerCase.trim
    }

    val fullPrefix = iteration match {
      case 1 => UrlKey.generate(prefix)
      case n => UrlKey.generate(s"$prefix-$n")
    }

    findAll(nickname = Some(fullPrefix)).headOption match {
      case None => fullPrefix
      case Some(org) => generateNickname(input, iteration + 1)
    }
  }

  private[db] def fromRow(
    row: anorm.Row,
    prefix: Option[String] = None
  ) = {
    val p = prefix.map( _ + "_").getOrElse("")
    User(
      guid = row[UUID](s"${p}guid"),
      email = row[String](s"${p}email"),
      nickname = row[String](s"${p}nickname"),
      name = row[Option[String]](s"${p}name"),
      audit = AuditsDao.fromRow(row, prefix)
    )
  }

}
