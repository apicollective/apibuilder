package core

import lib.{ServiceConfiguration, ServiceValidator}
import builder.OriginalValidator
import com.gilt.apidoc.api.v0.models.{Original, OriginalType}
import com.gilt.apidoc.spec.v0.models.{Service, ResponseCode, ResponseCodeOption, ResponseCodeUndefinedType, ResponseCodeInt}
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

  private lazy val specService: Service = {
    val config = ServiceConfiguration(
      orgKey = "gilt",
      orgNamespace = "com.gilt",
      version = "0.0.41"
    )

    val contents = readFile("spec/service.json")
    val validator = OriginalValidator(config, Original(OriginalType.ApiJson, contents), new MockServiceFetcher())
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
    migration: VersionMigration = VersionMigration(internal = false)
  ): ServiceValidatorForSpecs = {
    TestServiceValidator(
      OriginalValidator(
        serviceConfig,
        Original(OriginalType.ApiJson, contents),
        new MockServiceFetcher(),
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
      val version = com.gilt.apidoc.spec.v0.Constants.Version
      fetcher.add(s"http://localhost:9000/gilt/apidoc-spec/$version/service.json", specService)
      fetcher.add(s"http://www.apidoc.me/gilt/apidoc-spec/$version/service.json", specService)
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
