package controllers

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.WSClient

class ApplicationMetadataSpec extends PlaySpec with MockClient with GuiceOneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  private lazy val org = createOrganization()
  private lazy val application = {
    val a = createApplication(org)
    createVersion(a, version = "1.0.0")
    createVersion(a, version = "2.0.0")
    a
  }

  "GET /:orgKey/:applicationKey/metadata/versions" in {
    await(
      client.applications.getMetadataAndVersionsByApplicationKey(org.key, application.key)
    ).map(_.version) must equal(
      Seq("2.0.0", "1.0.0")
    )
  }

  "GET /:orgKey/:applicationKey/metadata/versions/latest.txt" in {
    val ws = app.injector.instanceOf[WSClient]
    val auth = sessionHelper.createAuthentication(testUser)

    val result = await(
      ws.url(
        s"http://localhost:$port/${org.key}/metadata/${application.key}/versions/latest.txt"
      ).addHttpHeaders(
        "Authorization" -> s"Session ${auth.session.id}"
      ).get()
    )
    result.status must equal(200)
    result.bodyAsBytes.utf8String must equal("2.0.0")
  }
}
