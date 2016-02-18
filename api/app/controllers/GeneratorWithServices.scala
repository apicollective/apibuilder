package controllers

import db.generators.GeneratorsDao
import com.bryzek.apidoc.api.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object GeneratorWithServices extends Controller with GeneratorWithServices

trait GeneratorWithServices {
  this: Controller =>

  def get(
    guid: Option[UUID],
    serviceGuid: Option[UUID],
    serviceUri: Option[String],
    key: Option[String],
    attributeName: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = AnonymousRequest { request =>
    val generators = GeneratorsDao.findAll(
      request.authorization,
      guid = guid,
      serviceGuid = serviceGuid,
      serviceUri = serviceUri,
      key = key,
      attributeName = attributeName,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(generators))
  }

  def getByKey(key: String) = AnonymousRequest { request =>
    GeneratorsDao.findAll(
      request.authorization,
      key = Some(key),
      limit = 1
    ).headOption match {
      case None => NotFound
      case Some(generator) => Ok(Json.toJson(generator))
    }
  }

}
