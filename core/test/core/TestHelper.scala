package core

import _root_.builder.OriginalValidator
import cats.data.ValidatedNec
import helpers.ValidatedTestHelpers
import io.apibuilder.api.json.v0.models.ApiJson
import io.apibuilder.api.json.v0.models.json._
import io.apibuilder.api.v0.models.OriginalType
import io.apibuilder.spec.v0.models._
import lib.{FileUtils, ServiceConfiguration, Text, ValidatedHelpers}
import play.api.libs.json.Json

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.UUID

object TestHelper extends ValidatedHelpers with ValidatedTestHelpers {

  def expectSingleError(json: String): String = {
    expectInvalid {
      TestHelper.serviceValidatorFromApiJson(json)
    }.toList match {
      case one :: Nil => {
        println(s"msg: $one")
        one
      }
      case other => sys.error(s"Expected 1 error but got[${other.length}")
    }
  }

  def expectSingleError(json: ApiJson): String = {
    expectSingleError(Json.prettyPrint(Json.toJson(json)))
  }

  val serviceConfig: ServiceConfiguration = ServiceConfiguration(
    orgKey = "test",
    orgNamespace = "test.apibuilder",
    version = "0.0.1-dev"
  )

  private val apibuilderConfig = ServiceConfiguration(
    orgKey = "apicollective",
    orgNamespace = "io",
    version = "0.0.41"
  )

  private val apiJsonValidator = OriginalValidator(apibuilderConfig, OriginalType.ApiJson, MockServiceFetcher())

  private lazy val specService: Service = {
    expectValid {
      apiJsonValidator.validate(readFile("spec/apibuilder-spec.json"))
    }
  }

  private lazy val commonService: Service = {
    expectValid {
      apiJsonValidator.validate(readFile("spec/apibuilder-common.json"))
    }
  }

  private lazy val generatorService: Service = {
    val fetcher = MockServiceFetcher()

    Seq(io.apibuilder.spec.v0.Constants.Version, "latest").foreach { version =>
      fetcher.add(s"http://app.apibuilder.io/apicollective/apibuilder-spec/$version/service.json", specService)
      fetcher.add(s"http://app.apibuilder.io/apicollective/apibuilder-common/$version/service.json", commonService)
      fetcher.add(s"https://app.apibuilder.io/apicollective/apibuilder-spec/$version/service.json", specService)
      fetcher.add(s"https://app.apibuilder.io/apicollective/apibuilder-common/$version/service.json", commonService)
    }

    expectValid {
      OriginalValidator(apibuilderConfig, OriginalType.ApiJson, fetcher).validate(readFile("spec/apibuilder-generator.json"))
    }
  }

  def responseCode(responseCode: ResponseCode): String = {
    responseCode match {
      case ResponseCodeInt(value) => value.toString
      case ResponseCodeOption.Default => ResponseCodeOption.Default.toString
      case ResponseCodeOption.UNDEFINED(value) => sys.error(s"invalid value[$value]")
      case ResponseCodeUndefinedType(value) => sys.error(s"invalid response code type[$value]")
    }
  }

  def serviceValidator(
    apiJson: ApiJson,
    fetcher: ServiceFetcher = FileServiceFetcher(),
  ): ValidatedNec[String, Service] = {
    serviceValidatorFromApiJson(
      contents = Json.toJson(apiJson).toString,
      fetcher = fetcher,
    )
  }

  def serviceValidatorFromApiJson(
    contents: String,
    migration: VersionMigration = VersionMigration(internal = false),
    fetcher: ServiceFetcher = MockServiceFetcher(),
  ): ValidatedNec[String, Service] = {
    OriginalValidator(
      serviceConfig,
      `type` = OriginalType.ApiJson,
      fetcher,
      migration
    ).validate(contents)
  }

  def writeToTempFile(contents: String): String = {
    val tmpPath = "/tmp/apibuilder.tmp." + UUID.randomUUID.toString
    writeToFile(tmpPath, contents)
    tmpPath
  }

  def writeToFile(path: String, contents: String): Unit = {
    val outputPath = Paths.get(path)
    val bytes = contents.getBytes(StandardCharsets.UTF_8)
    Files.write(outputPath, bytes)
  }

  def readFile(path: String): String = FileUtils.readToString(new File("../" + path))

  def parseFile(filename: String): ValidatedNec[String, Service] = {
    val fetcher = MockServiceFetcher()
    if (filename.indexOf("spec/apibuilder-api.json")>=0) {
      Seq(io.apibuilder.spec.v0.Constants.Version, "latest").foreach { version =>
        fetcher.add(s"/apibuilder-spec/$version/service.json", specService)
        fetcher.add(s"http://app.apibuilder.io/apicollective/apibuilder-common/$version/service.json", commonService)
        fetcher.add(s"http://app.apibuilder.io/apicollective/apibuilder-generator/$version/service.json", generatorService)
        fetcher.add(s"https://app.apibuilder.io/apicollective/apibuilder-spec/$version/service.json", specService)
        fetcher.add(s"https://app.apibuilder.io/apicollective/apibuilder-common/$version/service.json", commonService)
        fetcher.add(s"https://app.apibuilder.io/apicollective/apibuilder-generator/$version/service.json", generatorService)
      }
    }
    parseFile(filename, fetcher)
  }

  private def parseFile(
    filename: String,
    fetcher: ServiceFetcher
  ): ValidatedNec[String, Service] = {
    OriginalValidator(serviceConfig, OriginalType.ApiJson, fetcher).validate(readFile(filename))
  }

  def assertEqualsFile(filename: String, contents: String): Unit = {
    if (contents.trim != readFile(filename).trim) {
      val tmpPath = "/tmp/apibuilder.tmp." + Text.safeName(filename)
      TestHelper.writeToFile(tmpPath, contents.trim)
      sys.error(s"Test output did not match. diff $tmpPath $filename")
    }
  }

}
