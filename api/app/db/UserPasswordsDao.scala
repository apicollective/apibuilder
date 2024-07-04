package db

import io.apibuilder.api.v0.models.{Error, User}
import lib.Validation
import anorm._
import javax.inject.{Inject, Singleton}

import play.api.db._
import java.util.UUID
import java.sql.Connection

import io.flow.postgresql.Query

case class UserPassword(guid: UUID, userGuid: UUID, hashed: HashedValue)

@Singleton
class UserPasswordsDao @Inject() (
  @NamedDatabase("default") db: Database
) {

  private val MinLength = 5

  private val BaseQuery = Query(
    """
    select guid, user_guid, algorithm_key, hash
      from user_passwords
  """)

  private val InsertQuery =
    """
    insert into user_passwords
    (guid, user_guid, algorithm_key, hash, created_by_guid, updated_by_guid)
    values
    ({guid}::uuid, {user_guid}::uuid, {algorithm_key}, {hash}, {created_by_guid}::uuid, {updated_by_guid}::uuid)
  """

  private val SoftDeleteByUserGuidQuery =
    """
    update user_passwords
       set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now()
     where user_guid = {user_guid}::uuid
       and deleted_at is null
  """

  def validate(password: String): Seq[Error] = {
    val lengthErrors = if (password.trim.length < MinLength) {
      Seq(s"Password must be at least $MinLength characters")
    } else {
      Nil
    }

    Validation.errors(lengthErrors)
  }

  def create(user: User, userGuid: UUID, cleartextPassword: String): Unit = {
    db.withTransaction { implicit c =>
      softDeleteByUserGuid(c, user, userGuid)
      doCreate(c, user.guid, userGuid, cleartextPassword)
    }
  }

  private[db] def doCreate(
    implicit c: java.sql.Connection,
    creatingUserGuid: UUID,
    userGuid: UUID,
    cleartextPassword: String
  ): Unit = {
    val errors = validate(cleartextPassword)
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    val guid = UUID.randomUUID
    val algorithm = PasswordAlgorithm.Latest
    val hashedPassword = algorithm.hash(cleartextPassword)

    SQL(InsertQuery).on(
      "guid" -> guid,
      "user_guid" -> userGuid,
      "algorithm_key" -> algorithm.key,
      "hash" -> new String(Base64.encodeBase64(hashedPassword.hash.getBytes)),
      "created_by_guid" -> creatingUserGuid,
      "updated_by_guid" -> creatingUserGuid
    ).execute()
  }

  private def softDeleteByUserGuid(implicit c: Connection, user: User, userGuid: UUID): Unit = {
    SQL(SoftDeleteByUserGuidQuery).on("deleted_by_guid" -> user.guid, "user_guid" -> userGuid).execute()
  }

  def isValid(userGuid: UUID, cleartextPassword: String): Boolean = {
    findByUserGuid(userGuid) match {
      case None => false
      case Some(up: UserPassword) => up.algorithm.check(cleartextPassword, up.hash)
    }
  }

  private[db] def findByUserGuid(userGuid: UUID): Option[UserPassword] = {
    db.withConnection { implicit c =>
      BaseQuery.
        isNull("user_passwords.deleted_at").
        equals("user_passwords.user_guid", userGuid).
        limit(1).
        as(parser().*).headOption
    }
  }

  private def parser(): RowParser[UserPassword] = {
    SqlParser.get[UUID]("guid") ~
      SqlParser.get[UUID]("user_guid") ~
      SqlParser.str("algorithm_key") ~
      SqlParser.str("hash") map {
      case guid ~ userGuid ~ algorithmKey ~ hash => {
        UserPassword(
          guid = guid,
          userGuid = userGuid,
          algorithm = PasswordAlgorithm.fromString(algorithmKey).getOrElse {
            sys.error(s"Invalid algorithmKey[$algorithmKey] for userGuid[$userGuid]")
          },
          hash = new String(Base64.decodeBase64(hash.getBytes))
        )
      }
    }
  }
}
