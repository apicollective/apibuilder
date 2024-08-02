package controllers

import io.apibuilder.api.v0.models.json._
import db.InternalChangesDao
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._

@Singleton
class Changes @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  changesDao: InternalChangesDao
) extends ApiBuilderController {

  def get(
    orgKey: Option[String],
    applicationKey: Option[String],
    from: Option[String],
    to: Option[String],
    `type`: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ): Action[AnyContent] = Anonymous { request =>
    val changes = changesDao.findAll(
      request.authorization,
      organizationKey = orgKey,
      applicationKey = applicationKey,
      fromVersion = from,
      toVersion = to,
      `type` = `type`,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(changes))
  }

}
