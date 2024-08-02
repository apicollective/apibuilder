package controllers

import db.{InternalUser, InternalApplication, InternalOrganization}
import io.apibuilder.api.v0.models.{Organization, User, Watch, WatchForm}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class WatchesSpec extends PlaySpec with MockClient with GuiceOneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  private lazy val org: InternalOrganization = createOrganization()

  def createWatch(
    form: WatchForm = createWatchForm()
  ): Watch = {
    await(client.watches.post(form))
  }

  def createWatchForm(
    user: InternalUser = createUser(),
    org: InternalOrganization = createOrganization(),
    application: Option[InternalApplication] = None
  ): WatchForm = WatchForm(
    userGuid = user.guid,
    organizationKey = org.key,
    applicationKey = application.getOrElse(createApplication(org)).key
  )

  "POST /watches" in {
    val user = createUser()
    val application = createApplication(org)
    val watch = createWatch(
      WatchForm(
        organizationKey = org.key,
        userGuid = user.guid,
        applicationKey = application.key
      )
    )

    watch.user.guid must equal(user.guid)
    watch.organization.key must equal(org.key)
    watch.application.key must equal(application.key)
  }

  "POST /watches is idempotent" in {
    val user = createUser()
    val application = createApplication(org)

    val form = WatchForm(
      organizationKey = org.key,
      userGuid = user.guid,
      applicationKey = application.key
    )

    val watch = createWatch(form)
    val watch2 = createWatch(form)
    watch.guid mustBe watch2.guid
  }

  "GET /watches by application key" in {
    val org1 = createOrganization()
    val app1 = createApplication(org1)

    val org2 = createOrganization()
    val app2 = createApplication(org2, createApplicationForm(key = Some(app1.key)))
    val app3 = createApplication(org2)

    val user = createUser()

    def create(orgKey: String, appKey: String) = createWatch(WatchForm(user.guid, orgKey, appKey))

    create(org1.key, app1.key)
    create(org2.key, app2.key)
    create(org2.key, app3.key)

    def get(
             organizationKey: Option[String] = None,
             applicationKey: Option[String] = None,
           ): Seq[String] = {
      await(client.watches.get(
        userGuid = Some(user.guid),
        organizationKey = organizationKey,
        applicationKey = applicationKey)
      ).map(_.application.key).sorted
    }

    get() must equal(Seq(app1.key, app2.key, app3.key).sorted)
    get(applicationKey = Some(app1.key)) must equal(Seq(app1.key, app2.key).sorted)
    get(organizationKey = Some(org1.key), applicationKey = Some(app1.key)) must equal(Seq(app1.key))
  }

}
