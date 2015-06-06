package controllers

import db.{Authorization, ChangesDao, OrganizationsDao}
import com.bryzek.apidoc.api.v0.models.json._
import play.api.mvc._
import play.api.libs.json._

object Changes extends Controller with Changes

trait Changes {
  this: Controller =>

  def get(
    orgKey: Option[String],
    applicationKey: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = AnonymousRequest { request =>
    val changes = ChangesDao.findAll(
      request.authorization,
      organizationKey = orgKey,
      applicationKey = applicationKey,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(changes))
  }

}
