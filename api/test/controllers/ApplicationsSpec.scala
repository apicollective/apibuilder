package controllers

import db.ApplicationsDao
import com.gilt.apidoc.api.v0.models.{Application, ApplicationForm, MoveForm, Organization, Visibility}
import com.gilt.apidoc.api.v0.errors.{ErrorsResponse, UnitResponse}
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._

class ApplicationsSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  private def getByKey(org: Organization, key: String): Option[Application] = {
    await(client.applications.getByOrgKey(org.key, key = Some(key), limit = 1)).headOption
  }

  private lazy val org = db.Util.createOrganization(createdBy = TestUser)

  "POST /:orgKey" in new WithServer {
    val key = "test-" + UUID.randomUUID.toString
    val form = createApplicationForm(key = Some(key))
    createApplication(org, form).key must be(key)
  }

  "POST /:orgKey trims whitespace in name" in new WithServer {
    val name = UUID.randomUUID.toString
    createApplication(org, createApplicationForm(name = " " + name + " ")).name must be(name)
  }

  "POST /:orgKey validates key is valid" in new WithServer {
    intercept[ErrorsResponse] {
      createApplication(org, createApplicationForm(name = UUID.randomUUID.toString, key = Some("a")))
    }.errors.map(_.message) must be(Seq(s"Key must be at least 4 characters"))

    intercept[ErrorsResponse] {
      createApplication(org, createApplicationForm(name = UUID.randomUUID.toString, key = Some("a bad key")))
    }.errors.map(_.message) must be(Seq(s"Key must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid key would be: a-bad-key"))
  }

  "POST /:orgKey validates key is not reserved" in new WithServer {
    intercept[ErrorsResponse] {
      createApplication(org, createApplicationForm(name = "members", key = Some("members")))
    }.errors.map(_.message) must be(Seq(s"Prefix members is a reserved word and cannot be used for the key"))
  }

  /*
  "DELETE /:org/:key" in new WithServer {
    val application = createApplication(org)
    await(client.applications.deleteByOrgKeyAndApplicationKey(org.key, application.key)) must be(())
    getByKey(org, application.key) must be(None)
  }

  "GET /:orgKey" in new WithServer {
    val app1 = createApplication()
    val app2 = createApplication()

    await(client.applications.get(key = Some(UUID.randomUUID.toString))) must be(Seq.empty)
    await(client.applications.get(key = Some(app1.key))).head.guid must be(app1.guid)
    await(client.applications.get(key = Some(app2.key))).head.guid must be(app2.guid)
  }

  "GET /:orgKey/:key" in new WithServer {
    val app = createApplication()
    client.applications.getByKey(org, app.key).guid must be(app.guid)
    intercept[UnitResponse] {
      client.applications.getByKey(org, UUID.randomUUID.toString)
    }.status must be(404)
  }

  "GET /:orgKey for an anonymous user shows only public apps" in new WithServer {
    val privateApp = createApplication(createApplicationForm().copy(visibility = Visibility.Application)
    val publicApp = createApplication(createApplicationForm().copy(visibility = Visibility.Public)
    val anonymous = createUser()

    val client = newClient(anonymous)
    intercept[UnitResponse] {
      client.applications.getByKey(org, privateApp.key)
    }.status must be(404)
    client.applications.getByKey(org, publicApp.key).key must be(publicApp.key)

    await(client.applications.get(key = Some(privateApp.key)) must be(Seq.empty)
    await(client.applications.get(key = Some(publicApp.key)).map(_.key) must be(Seq(publicApp.key))
  }
   */

}
