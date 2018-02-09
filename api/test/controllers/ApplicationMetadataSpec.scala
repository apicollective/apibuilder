package controllers

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.ws.WSClient

class ApplicationMetadataSpec extends PlaySpec with MockClient with OneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] lazy val org = createOrganization()
  private[this] lazy val application = {
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
    val auth = sessionHelper.createAuthentication(TestUser)

    val result = await(
      ws.url(
        s"http://localhost:$defaultPort/${org.key}/metadata/${application.key}/versions/latest.txt"
      ).withHeaders(
        "Authorization" -> s"Session ${auth.session.id}"
      ).get()
    )
    result.status must equal(200)
    result.body must equal("2.0.0")
  }
}
