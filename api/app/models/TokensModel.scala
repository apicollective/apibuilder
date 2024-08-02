package models

import db.{InternalToken, InternalUsersDao}
import io.apibuilder.api.v0.models.Token

import javax.inject.Inject

class TokensModel @Inject()(usersModel: UsersModel) {

  def toModel(mr: InternalToken): Option[Token] = {
    toModels(Seq(mr)).headOption
  }

  def toModels(tokens: Seq[InternalToken]): Seq[Token] = {
    val users = usersModel.toModelByGuids(tokens.map(_.userGuid)).map { u => u.guid -> u }.toMap

    tokens.flatMap { t =>
      users.get(t.userGuid).map { user =>
        Token(
          guid = t.guid,
          maskedToken = t.maskedToken,
          description = t.description,
          user = user,
          audit = t.audit
        )
      }
    }
  }                                }