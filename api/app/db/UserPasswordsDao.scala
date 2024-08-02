package db

import anorm.*
import cats.implicits._
import cats.data.ValidatedNec
import cats.data.Validated.{Invalid, Valid}
import com.mbryzek.cipher.{CipherLibraryMindrot, Ciphers, HashedValue}
import io.apibuilder.api.v0.models.{Error, User}
import io.flow.postgresql.Query
import lib.Validation
import play.api.db.*

import java.sql.Connection
import java.util.UUID
import javax.inject.{Inject, Singleton}

case class UserPassword(guid: UUID, userGuid: UUID, algorithmKey: String, base64EncodedHash: String)

@Singleton
class UserPasswordsDao @Inject() (
  @NamedDatabase("default") db: Database
) {

  private val ciphers: Ciphers = Ciphers()
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

  def validate(password: String): ValidatedNec[Error, Unit] = {
    if (password.trim.length < MinLength) {
      Validation.singleError(s"Password must be at least $MinLength characters").invalidNec
    } else {
      ().validNec
    }
  }

  def create(user: InternalUser, userGuid: UUID, cleartextPassword: String): Unit = {
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
    validate(cleartextPassword) match {
      case Invalid(e) => sys.error("Invalid: " + e.toNonEmptyList.toList.map(_.message).mkString("\n"))
      case Valid(_) => // no-op
    }

    val guid = UUID.randomUUID
    val algorithm = ciphers.latest
    val hashedPassword = algorithm.hash(cleartextPassword)

    SQL(InsertQuery).on(
      "guid" -> guid,
      "user_guid" -> userGuid,
      "algorithm_key" -> algorithm.key,
      "hash" -> hashedPassword.hash,
      "created_by_guid" -> creatingUserGuid,
      "updated_by_guid" -> creatingUserGuid
    ).execute()
  }

  private def softDeleteByUserGuid(implicit c: Connection, user: InternalUser, userGuid: UUID): Unit = {
    SQL(SoftDeleteByUserGuidQuery).on("deleted_by_guid" -> user.guid, "user_guid" -> userGuid).execute()
  }

  def isValid(userGuid: UUID, cleartextPassword: String): Boolean = {
    findByUserGuid(userGuid) match {
      case None => false
      case Some(up) => {
        ciphers.libraries.find(_.key == up.algorithmKey) match {
          case None => false
          case Some(al) => {
            al.isValid(
              plaintext = cleartextPassword,
              hash = up.base64EncodedHash,
              salt = None
            )
          }
        }
      }
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
          algorithmKey = algorithmKey,
          base64EncodedHash = hash
        )
      }
    }
  }
}
