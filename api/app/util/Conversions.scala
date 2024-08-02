package util

import db.InternalUser
import io.apibuilder.api.v0.models.{Authentication, Session, User}
import models.UsersModel
import javax.inject.Inject

class Conversions @Inject() (
  usersModel: UsersModel
) {

  private def toSession(db: _root_.db.generated.Session): Session = {
    Session(
      id = db.id,
      expiresAt = db.expiresAt
    )
  }

  def toAuthentication(dbSession: _root_.db.generated.Session, user: InternalUser): Authentication = {
    Authentication(
      session = toSession(dbSession),
      user = usersModel.toModel(user)
    )
  }

}
