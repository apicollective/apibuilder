package lib

import builder.OriginalValidator
import db.Authorization
import helpers.ValidatedTestHelpers
import io.apibuilder.api.v0.models.OriginalType
import io.apibuilder.spec.v0.models.Service
import play.api.Application

import java.io.File

trait TestHelper extends ValidatedTestHelpers {

  def app: Application

  def readFile(path: String): String = FileUtils.readToString(new File(path))

  def readService(path: String): Service = {
    val config = ServiceConfiguration(
      orgKey = "gilt",
      orgNamespace = "io.apibuilder",
      version = "0.9.10"
    )

    val validator = OriginalValidator(
      config,
      OriginalType.ApiJson,
      app.injector.instanceOf[DatabaseServiceFetcher].instance(Authorization.All)
    )
    expectValid {
      validator.validate(readFile(path))
    }
  }

}

