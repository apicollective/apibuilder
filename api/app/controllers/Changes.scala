package controllers

import com.bryzek.apidoc.api.v0.models.json._
import db.ChangesDao
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._

@Singleton
class Changes @Inject() (
  changesDao: ChangesDao
) extends Controller {

  def get(
    orgKey: Option[String],
    applicationKey: Option[String],
    from: Option[String],
    to: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = AnonymousRequest { request =>
    val changes = changesDao.findAll(
      request.authorization,
      organizationKey = orgKey,
      applicationKey = applicationKey,
      fromVersion = from,
      toVersion = to,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(changes))
  }

}
