package db

import cats.implicits.*
import cats.data.ValidatedNec
import db.generated.TokensDao
import io.apibuilder.api.v0.models.{CleartextToken, Error, TokenForm, User}
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}
import io.flow.postgresql.{OrderBy, Query}
import lib.{TokenGenerator, Validation}
import play.api.db.*

import java.util.UUID
import javax.inject.{Inject, Singleton}

case class InternalToken(db: generated.Token) {
  val guid: UUID = db.guid
  val description: Option[String] = db.description
  val userGuid: UUID = db.userGuid

  val maskedToken: String = "XXX-XXX-XXX"
}

class InternalTokensDao @Inject()(
  dao: TokensDao,
  usersDao: InternalUsersDao
) {

  private def validateAuth(user: InternalUser, form: TokenForm): ValidatedNec[Error, Unit] = {
    if (user.guid == form.userGuid) {
      ().validNec
    } else {
      Validation.singleError("You are not authorized to create a token for this user").invalidNec
    }
  }

  private def validate(
                        user: InternalUser,
                        form: TokenForm
                      ): ValidatedNec[Error, Unit] = {
    (
      validateAuth(user, form),
      usersDao.findByGuid(form.userGuid).toValidNec(Validation.singleError("User not found"))
    ).mapN { case (_, _) => () }
  }

  def create(user: InternalUser, form: TokenForm): ValidatedNec[Error, InternalToken] = {
    validate(user, form).map { _ =>
      val guid = dao.insert(user.guid, generated.TokenForm(
        userGuid = form.userGuid,
        token = TokenGenerator.generate(),
        description = form.description.map(_.trim).filterNot(_.isEmpty)
      ))

      findByGuid(Authorization.All, guid).getOrElse {
        sys.error("Failed to create token")
      }
    }
  }

  def softDelete(deletedBy: InternalUser, token: InternalToken): Unit = {
    dao.delete(deletedBy.guid, token.db)
  }

  def findByToken(token: String): Option[InternalToken] = {
    findAll(Authorization.All, token = Some(token), limit = Some(1)).headOption
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[InternalToken] = {
    findAll(authorization, guid = Some(guid), limit = Some(1)).headOption
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    userGuid: Option[UUID] = None,
    token: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[InternalToken] = {
    dao.findAll(
      guid = guid,
      userGuid = userGuid,
      token = token,
      limit = limit,
      offset = offset,
      orderBy = Some(OrderBy("created_at"))
    ) { q =>
      authorization.tokenFilter(q)
        .and(isDeleted.map(Filters.isDeleted("tokens", _)))
    }.map(InternalToken(_))
  }
}
