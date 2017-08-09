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

  "GET /:orgKey/:applicationKey/metadata/versions" in new WithServer(port = defaultPort) {
    await(
      client.applications.getMetadataAndVersionsByApplicationKey(org.key, application.key)
    ).map(_.version) must beEqualTo(
      Seq("2.0.0", "1.0.0")
    )
  }

  "GET /:orgKey/:applicationKey/metadata/versions/latest.txt" in new WithServer(port = defaultPort) {
    val ws = app.injector.instanceOf[WSClient]
    val auth = sessionHelper.createAuthentication(TestUser)

    val result = await(
      ws.url(
        s"http://localhost:$defaultPort/${org.key}/metadata/${application.key}/versions/latest.txt"
      ).withHeaders(
        "Authorization" -> s"Session ${auth.session.id}"
      ).get()
    )
    result.status must beEqualTo(200)
    result.body must beEqualTo("2.0.0")
  }
}
