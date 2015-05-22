package controllers

import db.{Authorization, ChangesDao, OrganizationsDao}
import com.gilt.apidoc.internal.v0.models.json._
import play.api.mvc._
import play.api.libs.json._

object Changes extends Controller with Changes

trait Changes {
  this: Controller =>

  def getByOrgKey(
    orgKey: String,
    applicationKey: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Authenticated { request =>
    OrganizationsDao.findByKey(request.authorization, orgKey) match {
      case None => NotFound
      case Some(org) => {
        val changes = ChangesDao.findAll(
          request.authorization,
          organizationGuid = Some(org.guid),
          applicationKey = applicationKey,
          limit = limit,
          offset = offset
        )
        Ok(Json.toJson(changes))
      }
    }
  }

}
