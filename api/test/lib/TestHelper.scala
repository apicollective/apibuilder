package lib

import db.Authorization
import builder.OriginalValidator
import io.apibuilder.api.v0.models.{Original, OriginalType}
import io.apibuilder.spec.v0.models.Service
import play.api.Application

import java.io.File

trait TestHelper {

  def app: Application

  def readFile(path: String): String = FileUtils.readToString(new File(path))

  def readService(path: String): Service = {
    val config = ServiceConfiguration(
      orgKey = "gilt",
      orgNamespace = "io.apibuilder",
      version = "0.9.10"
    )

    val contents = readFile(path)
    val validator = OriginalValidator(
      config,
      Original(OriginalType.ApiJson, contents),
      app.injector.instanceOf[DatabaseServiceFetcher].instance(Authorization.All)
    )
    validOrError {}
    validator.validate() match {
      case Left(errors) => sys.error(s"Errors: $errors")
      case Right(service) => service
    }
  }

}

