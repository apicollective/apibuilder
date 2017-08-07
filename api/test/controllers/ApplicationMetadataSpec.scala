package controllers

import play.api.test._

class ApplicationMetadataSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] lazy val org = createOrganization()
  private[this] lazy val application = createApplication(org)
  private[this] lazy val version1 = createVersion(application, version = "1.0.0")
  private[this] lazy val version2 = createVersion(application, version = "2.0.0")

  "GET /:orgKey/metadata/:applicationKey/versions" in new WithServer(port = defaultPort) {
    await(
      client.applicationMetadata.getVersions(org.key, application.key)
    ).map(_.version) must beEqualTo(
      Seq("2.0.0", "1.0.0")
    )
  }

  "GET /:orgKey/metadata/:applicationKey/versions/latest" in new WithServer(port = defaultPort) {
    await(
      client.applicationMetadata.getVersionsAndLatestTxt(org.key, application.key)
    ) must beEqualTo("2.0.0")
  }
}
