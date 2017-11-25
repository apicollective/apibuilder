package controllers

import io.apibuilder.api.v0.models.json._
import util.Conversions
import db.UsersDao
import db.generated.SessionsDao
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc._


@Singleton
class Authentications @Inject() (
  sessionsDao: SessionsDao,
  usersDao: UsersDao
) extends Controller {

  def getSessionById(sessionId: String) = AnonymousRequest { _ =>
    sessionsDao.findById(sessionId) match {
      case None => NotFound
      case Some(session) => {
        if (session.deletedAt.isDefined) {
          NotFound
        } else {
          usersDao.findByGuid(session.userGuid) match {
            case None => NotFound
            case Some(user) => {
              Ok(Json.toJson(Conversions.toAuthentication(session, user)))
            }
          }
        }
      }
    }
  }

}
