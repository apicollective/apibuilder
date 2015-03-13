package core

import builder.ServiceValidator
import com.gilt.apidoc.v0.models.{Original, OriginalType}
import com.gilt.apidoc.spec.v0.models.Service
import lib.Text
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets
import java.util.UUID

object TestHelper {

  trait ServiceValidatorForSpecs extends ServiceValidator {
    def service(): Service
  }

  /**
    * Exposes a 'service' method to simplify access to service object
    * in tests
    */
  case class TestServiceValidator(validator: ServiceValidator) extends ServiceValidatorForSpecs {

    override def validate() = validator.validate()
    override def errors() = validator.errors()
    override def isValid = validator.isValid

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
    val validator = ServiceValidator(config, Original(OriginalType.ApiJson, contents), new MockServiceFetcher())
    TestServiceValidator(validator).service
  }

  def serviceValidatorFromApiJson(contents: String): ServiceValidatorForSpecs = {
    TestServiceValidator(
      ServiceValidator(serviceConfig, Original(OriginalType.ApiJson, contents), new MockServiceFetcher())
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
    val validator = ServiceValidator(serviceConfig, Original(OriginalType.ApiJson, contents), fetcher)
    if (!validator.isValid) {
      sys.error(s"Invalid api.json file[$filename]: " + validator.errors.mkString("\n"))
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
