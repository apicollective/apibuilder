package controllers

import cats.data.Validated.{Invalid, Valid}
import io.apibuilder.api.v0.models.BatchVersionsLatestForm
import io.apibuilder.api.v0.models.json.*
import lib.Validation
import play.api.libs.json.Json
import play.api.mvc.Action
import services.BatchVersionsLatestService

import javax.inject.{Inject, Singleton}

@Singleton
class BatchVersionsLatest @Inject() (
  override val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  service: BatchVersionsLatestService,
) extends ApiBuilderController {

  def post(orgKey: String): Action[BatchVersionsLatestForm] = Anonymous(parse.json[BatchVersionsLatestForm]) { request =>
    service.process(orgKey, request.body) match {
      case Valid(result) => Ok(Json.toJson(result))
      case Invalid(errors) => Conflict(Json.toJson(Validation.errors(errors)))
    }
  }

}
