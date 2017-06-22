package controllers

import com.bryzek.apidoc.api.v0.models.json._
import util.Conversions
import db.UsersDao
import db.generated.SessionsDao
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc._
import scala.concurrent.Future

@Singleton
class Authentications @Inject() (
  sessionsDao: SessionsDao,
  usersDao: UsersDao
) extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  def getSessionById(sessionId: String) = AnonymousRequest { request =>
    sessionsDao.findById(sessionId) match {
      case None => NotFound
      case Some(session) => {
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
