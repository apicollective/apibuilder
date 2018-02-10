package controllers

import io.apibuilder.api.v0.models.{Original, OriginalType, Validation}
import io.apibuilder.api.v0.models.json._
import core.ServiceFetcher
import lib.{DatabaseServiceFetcher, OriginalUtil, ServiceConfiguration}
import javax.inject.{Inject, Singleton}
import builder.OriginalValidator
import play.api.mvc._
import play.api.libs.json._

@Singleton
class Validations @Inject() (
  databaseServiceFetcher: DatabaseServiceFetcher
) extends Controller {

  private[this] val config = ServiceConfiguration(
    orgKey = "tmp",
    orgNamespace = "tmp.validations",
    version = "0.0.1-dev"
  )

  def post() = AnonymousRequest(parse.temporaryFile) { request =>
    val contents = scala.io.Source.fromFile(request.body.file, "UTF-8").getLines.mkString("\n")
    OriginalUtil.guessType(contents) match {
      case None => {
        BadRequest(Json.toJson(Validation(false, Seq("Could not determine the type of file from the content."))))
      }

      case Some(fileType) => {
        OriginalValidator(
          config = config,
          original = Original(fileType, contents),
          fetcher = databaseServiceFetcher.instance(request.authorization)
        ).validate match {
          case Left(errors) => {
            BadRequest(Json.toJson(Validation(false, errors)))
          }
          case Right(_) => {
            Ok(Json.toJson(Validation(true, Nil)))
          }
        }
      }
    }
  }

}
