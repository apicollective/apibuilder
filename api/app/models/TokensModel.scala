package models

import db.{InternalToken, UsersDao}
import io.apibuilder.api.v0.models.Token

import javax.inject.Inject

class TokensModel @Inject()(
                                        usersDao: UsersDao
                                        ) {
  def toModel(mr: InternalToken): Option[Token] = {
    toModels(Seq(mr)).headOption
  }

  def toModels(tokens: Seq[InternalToken]): Seq[Token] = {
    val users = usersDao.findAll(
      guids = Some(tokens.map(_.userGuid))
    ).map { u => u.guid -> u }.toMap

    tokens.flatMap { t =>
      users.get(t.userGuid).map { user =>
        Token(
          guid = t.guid,
          maskedToken = t.maskedToken,
          user = user,
          audit = t.audit
        )
      }
    }
  }                                }