package db

import cats.data.ValidatedNec
import cats.implicits.*
import com.mbryzek.cipher.Ciphers
import db.generated.UserPasswordsDao
import io.apibuilder.api.v0.models.Error
import lib.Validation

import java.sql.Connection
import java.util.UUID
import javax.inject.Inject

case class InternalUserPassword(db: generated.UserPassword) {
  val guid: UUID = db.guid
  val userGuid: UUID = db.userGuid
  val algorithmKey: String = db.algorithmKey
  val base64EncodedHash: String = db.hash
}

case class ValidatedPassword(cleartext: String)

class InternalUserPasswordsDao @Inject()(
  dao: UserPasswordsDao
) {

  private val ciphers: Ciphers = Ciphers()
  private val MinLength = 5

  private[db] def validate(password: String): ValidatedNec[Error, ValidatedPassword] = {
    val trimmed = password.trim
    if (trimmed.length < MinLength) {
      Validation.singleError(s"Password must be at least $MinLength characters").invalidNec
    } else {
      ValidatedPassword(trimmed).validNec
    }
  }

  def create(user: InternalUser, userGuid: UUID, cleartextPassword: String): ValidatedNec[Error, Unit] = {
    validate(cleartextPassword).map { vPwd =>
      dao.db.withTransaction { implicit c =>
        dao.deleteAllByUserGuid(c, user.guid, userGuid)
        doCreate(user.guid, userGuid, vPwd)(c)
      }
    }
  }

  private[db] def doCreate(
    creatingUserGuid: UUID,
    userGuid: UUID,
    pwd: ValidatedPassword
  )(c: java.sql.Connection): Unit = {
    val algorithm = ciphers.latest
    val hashedPassword = algorithm.hash(pwd.cleartext)

    dao.insert(c, creatingUserGuid, generated.UserPasswordForm(
      userGuid = userGuid,
      algorithmKey = algorithm.key,
      hash = hashedPassword.hash
    ))
    ()
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

  private[db] def findByUserGuid(userGuid: UUID): Option[InternalUserPassword] = {
    findAll(
      userGuid = Some(userGuid),
      isDeleted = Some(false),
      limit = Some(1)
    ).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    userGuid: Option[UUID] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long]
  ): Seq[InternalUserPassword] = {
    dao.findAll(
      guid = guid,
      userGuid = userGuid,
      limit = limit
    ) { q =>
      q.and(isDeleted.map(Filters.isDeleted("user_passwords", _)))
    }.map(InternalUserPassword(_))
  }
}
