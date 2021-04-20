package controllers

import io.apibuilder.api.v0.models.{Original, Validation}
import io.apibuilder.api.v0.models.json._
import lib.{DatabaseServiceFetcher, FileUtils, OriginalUtil, ServiceConfiguration}

import javax.inject.{Inject, Singleton}
import builder.OriginalValidator
import play.api.libs.Files
import play.api.mvc._
import play.api.libs.json._

@Singleton
class Validations @Inject() (
  val apibuilderControllerComponents: ApibuilderControllerComponents,
  databaseServiceFetcher: DatabaseServiceFetcher
) extends ApibuilderController {

  private[this] val config = ServiceConfiguration(
    orgKey = "tmp",
    orgNamespace = "tmp.validations",
    version = "0.0.1-dev"
  )

  def post(): Action[Files.TemporaryFile] = Anonymous(parse.temporaryFile) { request =>
    val contents = FileUtils.readToString(request.body.path.toFile)
    OriginalUtil.guessType(contents) match {
      case None => {
        UnprocessableEntity(Json.toJson(Validation(
          valid = false,
          errors = Seq("Could not determine the type of file from the content.")
        )))
      }

      case Some(fileType) => {
        OriginalValidator(
          config = config,
          original = Original(fileType, contents),
          fetcher = databaseServiceFetcher.instance(request.authorization)
        ).validate() match {
          case Left(errors) => {
            UnprocessableEntity(Json.toJson(Validation(
              valid = false,
              errors = errors
            )))
          }
          case Right(_) => {
            Ok(Json.toJson(Validation(
              valid = true,
              errors = Nil
            )))
          }
        }
      }
    }
  }

}
