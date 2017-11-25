package controllers

import io.apibuilder.api.v0.models.{Organization, Application, User, Watch, WatchForm}
import io.apibuilder.api.v0.errors.{ErrorsResponse, FailedRequest}
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._

class WatchesSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = organizationsDao.createWithAdministrator(TestUser, createOrganizationForm())

  def createWatch(
    form: WatchForm = createWatchForm()
  ): Watch = {
    await(client.watches.post(form))
  }

  def createWatchForm(
    user: User = createUser(),
    org: Organization = createOrganization(),
    application: Option[Application] = None
  ) = WatchForm(
    userGuid = user.guid,
    organizationKey = org.key,
    applicationKey = application.getOrElse(createApplication(org)).key
  )

  "POST /watches" in new WithServer(port=defaultPort) {
    val user = createUser()
    val application = createApplication(org)
    val watch = createWatch(
      WatchForm(
        organizationKey = org.key,
        userGuid = user.guid,
        applicationKey = application.key
      )
    )

    watch.user.guid must beEqualTo(user.guid)
    watch.organization.key must beEqualTo(org.key)
    watch.application.key must beEqualTo(application.key)
  }

  "POST /watches is idempotent" in new WithServer(port=defaultPort) {
    val user = createUser()
    val application = createApplication(org)

    val form = WatchForm(
      organizationKey = org.key,
      userGuid = user.guid,
      applicationKey = application.key
    )

    val watch = createWatch(form)
    createWatch(form)
  }

  "GET /watches by application key" in new WithServer(port=defaultPort) {
    val org1 = createOrganization()
    val application1 = createApplication(org1)

    val org2 = createOrganization()
    val application2 = createApplication(org2, createApplicationForm(key = Some(application1.key)))
    val application3 = createApplication(org2)

    val user = createUser()

    createWatch(WatchForm(user.guid, org1.key, application1.key))
    createWatch(WatchForm(user.guid, org2.key, application2.key))
    createWatch(WatchForm(user.guid, org2.key, application3.key))

    await(client.watches.get(userGuid = Some(user.guid))).map(_.application.key).sorted must beEqualTo(Seq(application1.key, application2.key, application3.key).sorted)
    await(client.watches.get(userGuid = Some(user.guid), applicationKey = Some(application1.key))).map(_.application.key).sorted must beEqualTo(Seq(application1.key, application2.key).sorted)
    await(client.watches.get(userGuid = Some(user.guid), organizationKey = Some(org1.key), applicationKey = Some(application1.key))).map(_.application.key).sorted must beEqualTo(Seq(application1.key).sorted)
  }

}
