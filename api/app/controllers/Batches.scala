package controllers

import cats.implicits._
import cats.data.Validated.{Invalid, Valid}
import io.apibuilder.api.v0.controllers.BatchesController
import io.apibuilder.api.v0.models.{BatchDownloadForm, BatchForm}
import io.apibuilder.api.v0.models.json._

import javax.inject.{Inject, Singleton}
import lib.Validation
import play.api.libs.json.Json
import play.api.mvc.Request
import services.BatchesService

import scala.concurrent.Future

@Singleton
class Batches @Inject()(
                         override val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                         service: BatchesService,
) extends ApiBuilderController with BatchesController {

  override def post(request: Request[BatchForm], orgKey: String, body: BatchForm): Future[Post] = {
    import Post._
    (for {
      auth <- authorizeUser(request)({ e => HTTP401(Seq(e)) })
      exp <- EitherT(
        service.post(auth, body)
      ).leftMap[Post]({ e => HTTP422(e) })
    } yield {
      HTTP201(exp)
    }).merge
  }

  override def postDownload(request: Request[BatchDownloadForm], orgKey: String, body: BatchDownloadForm): Future[PostDownload] = {


  }

  override def postDryrun(request: Request[BatchDownloadForm], orgKey: String, body: BatchDownloadForm): Future[PostDryrun] = {
    ???
  }
  def post(orgKey: String) = Anonymous(parse.json[BatchDownloadApplicationsForm]) { request =>
    service.process(request.authorization, orgKey, request.body) match {
      case Valid(result) => Created(Json.toJson(result))
      case Invalid(errors) => Conflict(Json.toJson(Validation.errors(errors)))
    }
  }

}
