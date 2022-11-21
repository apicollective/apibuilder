package core

import _root_.builder.OriginalValidator
import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec
import io.apibuilder.api.json.v0.models.ApiJson
import io.apibuilder.api.json.v0.models.json._
import io.apibuilder.api.v0.models.{Original, OriginalType}
import io.apibuilder.spec.v0.models._
import lib.{FileUtils, ServiceConfiguration, ServiceValidator, Text, ValidatedHelpers}
import play.api.libs.json.Json

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.UUID

object TestHelper extends ValidatedHelpers {

  trait ServiceValidatorForSpecs extends ServiceValidator[Service] {
    def service(): Service
    def errors(): Seq[String]
  }

  /**
    * Exposes a 'service' method to simplify access to service object
    * in tests
    */
  case class TestServiceValidator(validator: ServiceValidator[Service]) extends ServiceValidatorForSpecs {

    private[this] lazy val validateResult = validator.validate()

    override def validate(): ValidatedNec[String, Service] = validateResult

    override def errors(): Seq[String] = validateResult match {
      case Invalid(errors) => errors.toNonEmptyList.toList
      case Valid(_) => Seq.empty
    }

    override lazy val service: Service = validateResult match {
      case Invalid(errors) => sys.error(formatErrors(errors))
      case Valid(service) => service
    }

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

  private lazy val specService: Service = {
    val contents = readFile("spec/apibuilder-spec.json")
    val validator = OriginalValidator(apibuilderConfig, Original(OriginalType.ApiJson, contents), MockServiceFetcher())
    TestServiceValidator(validator).service
  }

  private lazy val commonService: Service = {
    val contents = readFile("spec/apibuilder-common.json")
    val validator = OriginalValidator(apibuilderConfig, Original(OriginalType.ApiJson, contents), MockServiceFetcher())
    TestServiceValidator(validator).service
  }

  private lazy val generatorService: Service = {
    val fetcher = MockServiceFetcher()

    Seq(io.apibuilder.spec.v0.Constants.Version, "latest").foreach { version =>
      fetcher.add(s"http://app.apibuilder.io/apicollective/apibuilder-spec/$version/service.json", specService)
      fetcher.add(s"http://app.apibuilder.io/apicollective/apibuilder-common/$version/service.json", commonService)
      fetcher.add(s"https://app.apibuilder.io/apicollective/apibuilder-spec/$version/service.json", specService)
      fetcher.add(s"https://app.apibuilder.io/apicollective/apibuilder-common/$version/service.json", commonService)
    }

    val contents = readFile("spec/apibuilder-generator.json")
    val validator = OriginalValidator(apibuilderConfig, Original(OriginalType.ApiJson, contents), fetcher)
    TestServiceValidator(validator).service
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
  ): ServiceValidatorForSpecs = {
    serviceValidatorFromApiJson(
      contents = Json.toJson(apiJson).toString,
      fetcher = fetcher,
    )
  }

  def serviceValidatorFromApiJson(
    contents: String,
    migration: VersionMigration = VersionMigration(internal = false),
    fetcher: ServiceFetcher = MockServiceFetcher(),
  ): ServiceValidatorForSpecs = {
    TestServiceValidator(
      OriginalValidator(
        serviceConfig,
        Original(OriginalType.ApiJson, contents),
        fetcher,
        migration
      )
    )
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

  def readFile(path: String): String = FileUtils.readToString(new File(path))

  def parseFile(filename: String): ServiceValidatorForSpecs = {
    val fetcher = MockServiceFetcher()
    if (filename == "spec/apibuilder-api.json") {
      Seq(io.apibuilder.spec.v0.Constants.Version, "latest").foreach { version =>
        fetcher.add(s"http://app.apibuilder.io/apicollective/apibuilder-spec/$version/service.json", specService)
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
  ): ServiceValidatorForSpecs = {
    val contents = readFile(filename)
    val validator = OriginalValidator(serviceConfig, Original(OriginalType.ApiJson, contents), fetcher)
    validator.validate() match {
      case Left(errors) => {
        sys.error(s"Invalid api.json file[$filename]: " + errors.mkString("\n"))
      }
      case Right(_) => {}
    }
    TestServiceValidator(
      validator
    )
  }

  def assertEqualsFile(filename: String, contents: String): Unit = {
    if (contents.trim != readFile(filename).trim) {
      val tmpPath = "/tmp/apibuilder.tmp." + Text.safeName(filename)
      TestHelper.writeToFile(tmpPath, contents.trim)
      sys.error(s"Test output did not match. diff $tmpPath $filename")
    }
  }

}
