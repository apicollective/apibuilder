package controllers

import com.gilt.apidoc.api.v0.models.{Application, Organization, OriginalForm, OriginalType, Version, VersionForm}
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._

class VersionsSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()
  lazy val application = createApplication(org)

  "POST /:orgKey/:version stores the original in the proper format" in new WithServer {
    val form = createVersionForm()
    val version = createVersion(application, form)

    // Now test that we stored the appropriate original
    version.original match {
      case None => fail("No original found")
      case Some(original) => {
        original.`type` must be(OriginalType.ApiJson)
        original.data must be(form.originalForm.data)
      }
    }
  }

}
