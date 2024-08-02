package models

import db.{Authorization, InternalUser, InternalUsersDao}
import io.apibuilder.api.v0.models.User
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}

import java.util.UUID
import javax.inject.Inject

class UsersModel @Inject()(
  usersDao: InternalUsersDao
) {

  def toModel(user: InternalUser): User = {
    toModels(Seq(user)).head
  }

  def toModelByGuids(guids: Seq[UUID]): Seq[User] = {
    toModels(usersDao.findAllByGuids(guids.distinct))
  }

  def toModels(users: Seq[InternalUser]): Seq[User] = {
    users.map { user =>
      User(
        guid = user.guid,
        email = user.email,
        name = user.name,
        nickname = user.nickname,
        audit = Audit(
          createdAt = user.db.createdAt,
          createdBy = ReferenceGuid(user.db.createdByGuid),
          updatedAt = user.db.updatedAt,
          updatedBy = ReferenceGuid(user.db.updatedByGuid),
        )
      )
    }
  }
}