package controllers

import db.generators.GeneratorsDao
import io.apibuilder.api.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
class GeneratorWithServices @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  generatorsDao: GeneratorsDao
) extends ApiBuilderController {

  def get(
    guid: Option[UUID],
    serviceGuid: Option[UUID],
    serviceUri: Option[String],
    attributeName: Option[String],
    key: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Anonymous { request =>
    val generators = generatorsDao.findAll(
      request.authorization,
      guid = guid,
      serviceGuid = serviceGuid,
      serviceUri = serviceUri,
      attributeName = attributeName,
      key = key,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(generators))
  }

  def getByKey(key: String) = Anonymous { request =>
    generatorsDao.findAll(
      request.authorization,
      key = Some(key),
      limit = 1
    ).headOption match {
      case None => NotFound
      case Some(generator) => Ok(Json.toJson(generator))
    }
  }

}
