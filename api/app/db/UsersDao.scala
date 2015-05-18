package db

import com.gilt.apidoc.api.v0.models.{Error, User, UserForm, UserUpdateForm}
import lib.{Constants, Misc, Role, UrlKey, Validation}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

object UsersDao {

  val AdminUserEmail = "admin@apidoc.me"

  lazy val AdminUser = UsersDao.findByEmail(AdminUserEmail).getOrElse {
    sys.error(s"Failed to find background user w/ email[$AdminUserEmail]")
  }

  private val BaseQuery = """
    select guid, email, name, nickname
      from users
     where true
  """

  private val InsertQuery = """
    insert into users
    (guid, email, name, nickname, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {email}, {name}, {nickname}, {created_by_guid}::uuid, {updated_by_guid}::uuid)
  """

  private val UpdateQuery = """
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
      case Some(pwd) => UserPasswordsDao.validate(pwd)
    }

    Validation.errors(emailErrors ++ nicknameErrors) ++ passwordErrors
  }

  def update(updatingUser: User, user: User, form: UserUpdateForm) {
    val errors = validate(form, existingUser = Some(user))
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    DB.withConnection { implicit c =>
      SQL(UpdateQuery).on(
        'guid -> user.guid,
        'email -> form.email.trim,
        'name -> form.name.map(_.trim),
        'nickname -> form.nickname.trim.toLowerCase,
        'updated_by_guid -> updatingUser.guid
      ).execute()
    }

    if (user.email.trim.toLowerCase != form.email.trim.toLowerCase) {
      EmailVerificationsDao.upsert(updatingUser, user, form.email)
    }

  }

  def create(form: UserForm): User = {
    val errors = validateNewUser(form)
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    val guid = UUID.randomUUID
    DB.withTransaction { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'email -> form.email.trim,
        'name -> form.name.map(_.trim),
        'nickname -> form.nickname.getOrElse(generateNickname(form.email)),
        'created_by_guid -> Constants.DefaultUserGuid,
        'updated_by_guid -> Constants.DefaultUserGuid
      ).execute()

      UserPasswordsDao.doCreate(c, guid, guid, form.password)
    }

    val user = findByGuid(guid).getOrElse {
      sys.error("Failed to create user")
    }

    global.Actors.mainActor ! actors.MainActor.Messages.UserCreated(guid)

    user
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
  private[db] def generateNickname(input: String, iteration: Int = 1): String = {
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
      name = row[Option[String]](s"${p}name")
    )
  }

}
