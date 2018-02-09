package lib

import db.Authorization
import builder.OriginalValidator
import io.apibuilder.api.v0.models.{Original, OriginalType}
import io.apibuilder.spec.v0.models.{Service, ResponseCode, ResponseCodeOption, ResponseCodeUndefinedType, ResponseCodeInt}

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

object TestHelper {

  def readFile(path: String): String = {
    scala.io.Source.fromFile(path).getLines.mkString("\n")
  }

  def readService(path: String): Service = {
    val config = ServiceConfiguration(
      orgKey = "gilt",
      orgNamespace = "io.apibuilder",
      version = "0.9.10"
    )

    val contents = readFile(path)
    val validator = OriginalValidator(config, Original(OriginalType.ApiJson, contents), DatabaseServiceFetcher(Authorization.All))
    validator.validate match {
      case Left(errors) => sys.error(s"Errors: $errors")
      case Right(service) => service
    }
  }

}

