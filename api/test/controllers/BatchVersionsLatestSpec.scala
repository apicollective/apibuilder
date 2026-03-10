package controllers

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec
import io.apibuilder.api.v0.models.BatchVersionsLatestForm

class BatchVersionsLatestSpec extends PlaySpec with MockClient with GuiceOneServerPerSuite {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  private lazy val org = createOrganization()
  private lazy val version = createVersion(createApplication(org))

  "post" must {
    "returns latest version for existing application" in {
      val result = await {
        client.batchVersionsLatest.post(
          org.key,
          BatchVersionsLatestForm(applicationKeys = Seq(version.application.key))
        )
      }
      result.applications.size must equal(1)
      result.applications.head.applicationKey must equal(version.application.key)
      result.applications.head.latestVersion must equal(Some(version.version))
    }

    "returns no version for non-existent application" in {
      val result = await {
        client.batchVersionsLatest.post(
          org.key,
          BatchVersionsLatestForm(applicationKeys = Seq(randomString()))
        )
      }
      result.applications.size must equal(1)
      result.applications.head.latestVersion must equal(None)
    }

    "handles multiple applications" in {
      val version2 = createVersion(createApplication(org))
      val result = await {
        client.batchVersionsLatest.post(
          org.key,
          BatchVersionsLatestForm(applicationKeys = Seq(version.application.key, version2.application.key))
        )
      }
      result.applications.size must equal(2)
      result.applications.map(_.applicationKey) must equal(Seq(version.application.key, version2.application.key))
      result.applications.foreach { app =>
        app.latestVersion mustBe defined
      }
    }

    "handles mix of existing and non-existent applications" in {
      val nonExistent = randomString()
      val result = await {
        client.batchVersionsLatest.post(
          org.key,
          BatchVersionsLatestForm(applicationKeys = Seq(version.application.key, nonExistent))
        )
      }
      result.applications.size must equal(2)
      result.applications.find(_.applicationKey == version.application.key).get.latestVersion must equal(Some(version.version))
      result.applications.find(_.applicationKey == nonExistent).get.latestVersion must equal(None)
    }

    "handles empty list" in {
      val result = await {
        client.batchVersionsLatest.post(
          org.key,
          BatchVersionsLatestForm(applicationKeys = Nil)
        )
      }
      result.applications must equal(Nil)
    }
  }

}
