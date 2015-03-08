package core

import builder.ServiceValidator
import com.gilt.apidoc.v0.models.{Original, OriginalType}
import com.gilt.apidoc.spec.v0.models.Service
import lib.Text
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets
import java.util.UUID

object TestHelper {

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
    ServiceValidator(config, Original(OriginalType.ApiJson, contents), new MockServiceFetcher()).service
  }

  def serviceValidatorFromApiJson(contents: String): ServiceValidator = {
    ServiceValidator(serviceConfig, Original(OriginalType.ApiJson, contents), new MockServiceFetcher())
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

  def parseFile(filename: String): ServiceValidator = {
    val fetcher = MockServiceFetcher()
    if (filename == "api/api.json") {
      val version = com.gilt.apidoc.spec.v0.Constants.Version
      fetcher.add(s"http://localhost:9000/gilt/apidoc-spec/$version/service.json", specService)
      fetcher.add(s"http://www.apidoc.me/gilt/apidoc-spec/$version/service.json", specService)
    }
    parseFile(filename, fetcher)
  }

  private def parseFile(
    filename: String,
    fetcher: ServiceFetcher
  ): ServiceValidator = {
    val contents = readFile(filename)
    val validator = ServiceValidator(serviceConfig, Original(OriginalType.ApiJson, contents), fetcher)
    if (!validator.isValid) {
      sys.error(s"Invalid api.json file[$filename]: " + validator.errors.mkString("\n"))
    }
    validator
  }

  def assertEqualsFile(filename: String, contents: String) {
    if (contents.trim != readFile(filename).trim) {
      val tmpPath = "/tmp/apidoc.tmp." + Text.safeName(filename)
      TestHelper.writeToFile(tmpPath, contents.trim)
      sys.error(s"Test output did not match. diff $tmpPath $filename")
    }
  }

}
