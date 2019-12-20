package core

import lib.{ServiceConfiguration, ServiceValidator}
import builder.OriginalValidator
import io.apibuilder.api.v0.models.{Original, OriginalType}
import io.apibuilder.spec.v0.models.{ResponseCode, ResponseCodeInt, ResponseCodeOption, ResponseCodeUndefinedType, Service}
import lib.Text
import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets
import java.util.UUID

object TestHelper {

  trait ServiceValidatorForSpecs extends ServiceValidator[Service] {
    def service(): Service
    def errors(): Seq[String]
  }

  /**
    * Exposes a 'service' method to simplify access to service object
    * in tests
    */
  case class TestServiceValidator(validator: ServiceValidator[Service]) extends ServiceValidatorForSpecs {

    override def validate() = validator.validate()

    override def errors() = validate match {
      case Left(errors) => errors
      case Right(_) => Seq.empty
    }

    override lazy val service: Service = validate match {
      case Left(errors) => sys.error(errors.mkString(", "))
      case Right(service) => service
    }

  }

  val serviceConfig = ServiceConfiguration(
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

  def serviceValidatorFromApiJson(
    contents: String,
    migration: VersionMigration = VersionMigration(internal = false),
    fetcher: MockServiceFetcher = MockServiceFetcher()
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

  def writeToFile(path: String, contents: String) {
    val outputPath = Paths.get(path)
    val bytes = contents.getBytes(StandardCharsets.UTF_8)
    Files.write(outputPath, bytes)
  }

  def readFile(path: String): String = {
    scala.io.Source.fromFile(path).getLines.mkString("\n")
  }

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
    validator.validate match {
      case Left(errors) => {
        sys.error(s"Invalid api.json file[$filename]: " + errors.mkString("\n"))
      }
      case Right(_) => {}
    }
    TestServiceValidator(
      validator
    )
  }

  def assertEqualsFile(filename: String, contents: String) {
    if (contents.trim != readFile(filename).trim) {
      val tmpPath = "/tmp/apibuilder.tmp." + Text.safeName(filename)
      TestHelper.writeToFile(tmpPath, contents.trim)
      sys.error(s"Test output did not match. diff $tmpPath $filename")
    }
  }

}
