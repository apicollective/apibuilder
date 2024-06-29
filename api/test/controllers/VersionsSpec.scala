package controllers

import io.apibuilder.api.v0.models.OriginalType
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec

class VersionsSpec extends PlaySpec with MockClient with GuiceOneServerPerSuite {

  private lazy val org = createOrganization()
  private lazy val application = createApplication(org)

  "POST /:orgKey/:version stores the original in the proper format" in {
    val form = createVersionForm(name = application.name)
    val version = createVersionThroughApi(application, Some(form))

    // Now test that we stored the appropriate original
    version.original match {
      case None => sys.error("No original found")
      case Some(original) => {
        original.`type` must equal(OriginalType.ApiJson)
        original.data must equal(form.originalForm.data)
      }
    }
  }

}
