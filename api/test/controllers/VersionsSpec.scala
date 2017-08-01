package controllers

import io.apibuilder.api.v0.models.OriginalType
import play.api.test._

class VersionsSpec extends PlaySpecification with MockClient {

  private[this] lazy val org = createOrganization()
  private[this] lazy val application = createApplication(org)

  "POST /:orgKey/:version stores the original in the proper format" in new WithServer(port=defaultPort) {
    val form = createVersionForm(name = application.name)
    val version = createVersion(application, Some(form))

    // Now test that we stored the appropriate original
    version.original match {
      case None => sys.error("No original found")
      case Some(original) => {
        original.`type` must beEqualTo(OriginalType.ApiJson)
        original.data must beEqualTo(form.originalForm.data)
      }
    }
  }

}
