package core

import lib.{ServiceConfiguration, ServiceValidator}
import builder.OriginalValidator
import com.bryzek.apidoc.api.v0.models.{Original, OriginalType}
import com.bryzek.apidoc.spec.v0.models.{Service, ResponseCode, ResponseCodeOption, ResponseCodeUndefinedType, ResponseCodeInt}
import lib.Text
import java.nio.file.{Paths, Files}
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
    orgNamespace = "test.apidoc",
    version = "0.0.1-dev"
  )

  private val bryzekConfig = ServiceConfiguration(
    orgKey = "bryzek",
    orgNamespace = "com.bryzek",
    version = "0.0.41"
  )

  private lazy val specService: Service = {
    val contents = readFile("spec/spec.json")
    val validator = OriginalValidator(bryzekConfig, Original(OriginalType.ApiJson, contents), new MockServiceFetcher())
    TestServiceValidator(validator).service
  }

  private lazy val commonService: Service = {
    val contents = readFile("spec/common.json")
    val validator = OriginalValidator(bryzekConfig, Original(OriginalType.ApiJson, contents), new MockServiceFetcher())
    TestServiceValidator(validator).service
  }

  private lazy val generatorService: Service = {
    val fetcher = MockServiceFetcher()
    val version = com.bryzek.apidoc.spec.v0.Constants.Version
    fetcher.add(s"http://www.apidoc.me/bryzek/apidoc-spec/$version/service.json", specService)
    fetcher.add(s"http://www.apidoc.me/bryzek/apidoc-common/$version/service.json", commonService)

    val contents = readFile("spec/generator.json")
    val validator = OriginalValidator(bryzekConfig, Original(OriginalType.ApiJson, contents), fetcher)
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
    fetcher: MockServiceFetcher = new MockServiceFetcher()
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
    val tmpPath = "/tmp/apidoc.tmp." + UUID.randomUUID.toString
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
    if (filename == "spec/api.json") {
      val version = com.bryzek.apidoc.spec.v0.Constants.Version
      fetcher.add(s"http://www.apidoc.me/bryzek/apidoc-spec/$version/service.json", specService)
      fetcher.add(s"http://www.apidoc.me/bryzek/apidoc-common/$version/service.json", commonService)
      fetcher.add(s"http://www.apidoc.me/bryzek/apidoc-generator/$version/service.json", generatorService)
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
      val tmpPath = "/tmp/apidoc.tmp." + Text.safeName(filename)
      TestHelper.writeToFile(tmpPath, contents.trim)
      sys.error(s"Test output did not match. diff $tmpPath $filename")
    }
  }

}
