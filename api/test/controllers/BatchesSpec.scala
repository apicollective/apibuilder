package controllers

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec

class BatchesSpec extends PlaySpec with MockClient with GuiceOneServerPerSuite
  with helpers.BatchDownloadApplicationHelpers
{

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  private[this] lazy val version = createVersion(createApplication(createOrganization()))

  "postApplications" must {
    def post(key: String) = {
      client.batchDownloadApplications.post(
        version.organization.key,
        makeBatchDownloadApplicationsForm(
          applications = Seq(
            makeBatchDownloadApplicationForm(key)
          )
        )
      )
    }

    "2xx" in {
      await { post(version.application.key) }
    }

    "409" in {
      expectErrors { post(randomString()) }
    }
  }

}
