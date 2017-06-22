package util

import com.bryzek.apidoc.api.v0.models.{Authentication, Session, User}

object Conversions {

  def toSession(db: _root_.db.generated.Session): Session = {
    Session(
      id = db.id,
      expiresAt = db.expiresAt
    )
  }

  def toAuthentication(dbSession: _root_.db.generated.Session, user: User): Authentication = {
    Authentication(
      session = toSession(dbSession),
      user = user
    )
  }

}
