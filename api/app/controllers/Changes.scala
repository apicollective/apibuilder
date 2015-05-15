package controllers

import db.{Authorization}
import lib.Validation
import com.gilt.apidoc.api.v0.models.{User, Change}
import com.gilt.apidoc.api.v0.models.json._
import play.api.mvc._
import play.api.libs.json._

object Changes extends Controller with Changes

trait Changes {
  this: Controller =>

  def getByOrgKey(
    organizationKey: String,
    applicationKey: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Authenticated { request =>
/*
    val changes = ChangesDao.findAll(
      request.authorization,
      organizationKey = Some(organizationKey),
      applicationKey = applicationKey,
      limit = limit,
      offset = offset
    )
 */
    Ok(Json.toJson("TODO"))
  }

}
