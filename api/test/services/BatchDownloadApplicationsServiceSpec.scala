package services

import controllers.MockClient
import db.Authorization
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class BatchDownloadApplicationsServiceSpec extends PlaySpec with MockClient with GuiceOneServerPerSuite
  with helpers.BatchDownloadApplicationHelpers
  with helpers.ValidatedTestHelpers
{
  private def batchDownloadApplicationsService: BatchDownloadApplicationsService = app.injector.instanceOf[BatchDownloadApplicationsService]

  "process multiple applications" in {
    val org = createOrganization()
    val version1 = createVersion(createApplication(org))
    val version2 = createVersion(createApplication(org))

    expectValid {
      batchDownloadApplicationsService.process(
        Authorization.All,
        org.key,
        makeBatchDownloadApplicationsForm(
          applications = Seq(
            makeBatchDownloadApplicationForm(version1.application.key),
            makeBatchDownloadApplicationForm(version2.application.key),
          )
        )
      )
    }.applications.map(_.application.key) must equal(
      Seq(version1.application.key, version2.application.key)
    )
  }

}
