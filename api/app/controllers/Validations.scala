package controllers

import io.apibuilder.api.v0.models.{Original, Validation}
import io.apibuilder.api.v0.models.json._
import lib.{DatabaseServiceFetcher, FileUtils, OriginalUtil, ServiceConfiguration}

import javax.inject.{Inject, Singleton}
import builder.OriginalValidator
import cats.data.Validated.{Invalid, Valid}
import play.api.libs.Files
import play.api.mvc._
import play.api.libs.json._

@Singleton
class Validations @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
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
          `type` = fileType,
          fetcher = databaseServiceFetcher.instance(request.authorization)
        ).validate(contents) match {
          case Invalid(errors) => {
            UnprocessableEntity(Json.toJson(Validation(
              valid = false,
              errors = errors.toNonEmptyList.toList
            )))
          }
          case Valid(_) => {
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
