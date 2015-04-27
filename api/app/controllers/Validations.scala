package controllers

import com.gilt.apidoc.api.v0.models.{Original, OriginalType, Validation}
import com.gilt.apidoc.api.v0.models.json._
import lib.ServiceConfiguration
import core.ClientFetcher
import builder.OriginalValidator
import play.api.mvc._
import play.api.libs.json._

object Validations extends Controller {

  private val config = ServiceConfiguration(
    orgKey = "tmp",
    orgNamespace = "tmp.validations",
    version = "0.0.1-dev"
  )

  def post() = AnonymousRequest(parse.temporaryFile) { request =>
    request.body.file.getName()
    val fileType = OriginalType.ApiJson // TODO
    val contents = scala.io.Source.fromFile(request.body.file, "UTF-8").getLines.mkString("\n")
    OriginalValidator(
      config = config,
      original = Original(fileType, contents),
      fetcher = ClientFetcher(requestHeaders = request.authHeaders.toSeq)
    ).validate match {
      case Left(errors) => {
        BadRequest(Json.toJson(Validation(false, errors)))
      }
      case Right(service) => {
        Ok(Json.toJson(Validation(true, Nil)))
      }
    }
  }

}
