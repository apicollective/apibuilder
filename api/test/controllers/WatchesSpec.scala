package controllers

import db.InternalApplication
import io.apibuilder.api.v0.models.{Organization, User, Watch, WatchForm}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class WatchesSpec extends PlaySpec with MockClient with GuiceOneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org: Organization = createOrganization()

  def createWatch(
    form: WatchForm = createWatchForm()
  ): Watch = {
    await(client.watches.post(form))
  }

  def createWatchForm(
    user: User = createUser(),
    org: Organization = createOrganization(),
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
    val application1 = createApplication(org1)

    val org2 = createOrganization()
    val application2 = createApplication(org2, createApplicationForm(key = Some(application1.key)))
    val application3 = createApplication(org2)

    val user = createUser()

    createWatch(WatchForm(user.guid, org1.key, application1.key))
    createWatch(WatchForm(user.guid, org2.key, application2.key))
    createWatch(WatchForm(user.guid, org2.key, application3.key))

    await(client.watches.get(userGuid = Some(user.guid))).map(_.application.key).sorted must equal(Seq(application1.key, application2.key, application3.key).sorted)
    await(client.watches.get(userGuid = Some(user.guid), applicationKey = Some(application1.key))).map(_.application.key).sorted must equal(Seq(application1.key, application2.key).sorted)
    await(client.watches.get(userGuid = Some(user.guid), organizationKey = Some(org1.key), applicationKey = Some(application1.key))).map(_.application.key).sorted must equal(Seq(application1.key).sorted)
  }

}
