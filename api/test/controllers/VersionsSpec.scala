package controllers

import com.gilt.apidoc.v0.models.{OriginalType, VersionForm}
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._

class VersionsSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()

  "POST /:orgKey/:version stores the original in the proper format" in new WithServer {
    val name = s"test-${UUID.randomUUID}"
    val serviceForm = s"""{ "name": "$name" }"""

    val version = await(
      client.versions.postByOrgKeyAndVersion(
        orgKey = org.key,
        version = "0.0.1",
        versionForm = com.gilt.apidoc.v0.models.VersionForm(
          serviceForm = serviceForm
        )
      )
    )

    // Now test that we stored the appropriate original
    version.original match {
      case None => fail("No original found")
      case Some(original) => {
        original.`type` must be(OriginalType.ApiJson)
        original.data must be(serviceForm)
      }
    }
  }

}
