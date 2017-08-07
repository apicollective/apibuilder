package controllers

import play.api.libs.ws.WSClient
import play.api.test._

class ApplicationMetadataSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] lazy val org = createOrganization()
  private[this] lazy val application = {
    val a = createApplication(org)
    createVersion(a, version = "1.0.0")
    createVersion(a, version = "2.0.0")
    a
  }

  "GET /:orgKey/metadata/:applicationKey/versions" in new WithServer(port = defaultPort) {
    await(
      client.applicationMetadata.getVersions(org.key, application.key)
    ).map(_.version) must beEqualTo(
      Seq("2.0.0", "1.0.0")
    )
  }

  "GET /:orgKey/metadata/:applicationKey/versions/latest" in new WithServer(port = defaultPort) {
    val ws = app.injector.instanceOf[WSClient]
    await(
      ws.url(
        s"http://localhost:$defaultPort/${org.key}/metadata/${application.key}/versions/latest"
      ).get()
    ).body must beEqualTo("2.0.0")
  }
}
