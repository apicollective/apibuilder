package controllers

import cats.data.Validated.{Invalid, Valid}
import io.apibuilder.api.v0.models.json._
import javax.inject.{Inject, Singleton}
import lib.Validation
import play.api.libs.json.Json
import services.BatchesService

@Singleton
class Batches @Inject()(
                         override val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                         service: BatchesService,
) extends ApiBuilderController with BatchCont {

  def post(orgKey: String) = Anonymous(parse.json[BatchDownloadApplicationsForm]) { request =>
    service.process(request.authorization, orgKey, request.body) match {
      case Valid(result) => Created(Json.toJson(result))
      case Invalid(errors) => Conflict(Json.toJson(Validation.errors(errors)))
    }
  }

}
