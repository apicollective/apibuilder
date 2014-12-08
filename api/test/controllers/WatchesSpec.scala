package controllers

import com.gilt.apidoc.FailedRequest
import com.gilt.apidoc.models.{Organization, Service, User, Watch, WatchForm}
import com.gilt.apidoc.error.ErrorsResponse
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._

class WatchesSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()

  def createWatch(
    form: WatchForm = createWatchForm()
  ): Watch = {
    await(client.watches.post(form))
  }

  def createWatchForm(
    user: User = createUser(),
    org: Organization = createOrganization(),
    service: Option[Service] = None
  ) = WatchForm(
    userGuid = user.guid,
    organizationKey = org.key,
    serviceKey = service.getOrElse(createService(org)).key
  )

  "POST /watches" in new WithServer {
    val user = createUser()
    val service = createService(org)
    val watch = createWatch(
      WatchForm(
        organizationKey = org.key,
        userGuid = user.guid,
        serviceKey = service.key
      )
    )

    watch.user.guid must be(user.guid)
    watch.organization.key must be(org.key)
    watch.service.key must be(service.key)
  }

}
