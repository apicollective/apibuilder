package controllers

import db.generators.GeneratorsDao
import com.gilt.apidoc.api.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object Generators extends Controller with Generators

trait Generators {
  this: Controller =>

  def get(
    guid: Option[_root_.java.util.UUID],
    serviceUri: Option[String],
    key: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = AnonymousRequest { request =>
    val generators = GeneratorsDao.findAll(
      request.authorization,
      guid = guid,
      serviceUri = serviceUri,
      key = key,
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
