package controllers

import db.generators.InternalGeneratorsDao
import io.apibuilder.api.v0.models.json._
import models.GeneratorWithServiceModel
import play.api.mvc._
import play.api.libs.json._

import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
class GeneratorWithServices @Inject() (
                                        val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                                        generatorsDao: InternalGeneratorsDao,
                                        model: GeneratorWithServiceModel
) extends ApiBuilderController {

  def get(
    guid: Option[UUID],
    serviceGuid: Option[UUID],
    serviceUri: Option[String],
    attributeName: Option[String],
    key: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ): Action[AnyContent] = Anonymous { request =>
    val generators = generatorsDao.findAll(
      guid = guid,
      serviceGuid = serviceGuid,
      serviceUri = serviceUri,
      attributeName = attributeName,
      key = key,
      limit = Some(limit),
      offset = offset
    )
    Ok(Json.toJson(model.toModels(generators)))
  }

  def getByKey(key: String): Action[AnyContent] = Anonymous { request =>
    generatorsDao.findAll(
      key = Some(key),
      limit = Some(1)
    ).headOption.flatMap(model.toModel) match {
      case None => NotFound
      case Some(gws) => Ok(Json.toJson(gws))
    }
  }

}
