package controllers

import db.ApplicationsDao
import com.bryzek.apidoc.api.v0.models.{Application, ApplicationForm, MoveForm, Organization, Visibility}
import com.bryzek.apidoc.api.v0.errors.{ErrorsResponse, UnitResponse}
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

  "DELETE /:org/:key" in new WithServer {
    val application = createApplication(org)
    await(client.applications.deleteByOrgKeyAndApplicationKey(org.key, application.key)) must be(())
    getByKey(org, application.key) must be(None)
  }

  "GET /:orgKey by application name" in new WithServer {
    val app1 = createApplication(org)
    val app2 = createApplication(org)

    await(client.applications.getByOrgKey(org.key, name = Some(UUID.randomUUID.toString))) must be(Seq.empty)
    await(client.applications.getByOrgKey(org.key, name = Some(app1.name))).map(_.guid) must be(Seq(app1.guid))
    await(client.applications.getByOrgKey(org.key, name = Some(app2.name.toUpperCase))).map(_.guid) must be(Seq(app2.guid))
  }

  "GET /:orgKey by application key" in new WithServer {
    val app1 = createApplication(org)
    val app2 = createApplication(org)

    await(client.applications.getByOrgKey(org.key, key = Some(UUID.randomUUID.toString))) must be(Seq.empty)
    await(client.applications.getByOrgKey(org.key, key = Some(app1.key))).map(_.guid) must be(Seq(app1.guid))
    await(client.applications.getByOrgKey(org.key, key = Some(app2.key.toUpperCase))).map(_.guid) must be(Seq(app2.guid))
  }

  "GET /:orgKey by hasVersion" in new WithServer {
    val org = createOrganization()
    val app1 = createApplication(org)
    val app2 = createApplication(org)
    val version = createVersion(app2)

    await(client.applications.getByOrgKey(org.key, hasVersion = None)).map(_.key).sorted must be(Seq(app2.key, app1.key).sorted)
    await(client.applications.getByOrgKey(org.key, hasVersion = Some(false))).map(_.key) must be(Seq(app1.key))
    await(client.applications.getByOrgKey(org.key, hasVersion = Some(true))).map(_.key) must be(Seq(app2.key))
  }

  "POST /:orgKey/:applicationKey/move" in new WithServer {
    val org2 = createOrganization()
    val application = createApplication(org)

    val updated = await(client.applications.postMoveByOrgKeyAndApplicationKey(org.key, application.key, MoveForm(orgKey = org2.key)))
    updated.organization.guid must be(org2.guid)
  }

  "POST /:orgKey/:applicationKey/move validates org key" in new WithServer {
    val application = createApplication(org)
    val key = UUID.randomUUID.toString

    intercept[ErrorsResponse] {
      await(client.applications.postMoveByOrgKeyAndApplicationKey(org.key, application.key, MoveForm(orgKey = key)))
    }.errors.map(_.message) must be(Seq(s"Organization[$key] not found"))
  }

  "POST /:orgKey/:applicationKey/move validates duplicate application key" in new WithServer {
    val application = createApplication(org)
    val org2 = createOrganization()
    createApplication(org2, createApplicationForm().copy(key = Some(application.key)))

    // Test no-op if moving own app
    await(client.applications.postMoveByOrgKeyAndApplicationKey(org.key, application.key, MoveForm(orgKey = org.key)))

    // Test validation if org key already defined for the org to which we are moving the app
    intercept[ErrorsResponse] {
      await(client.applications.postMoveByOrgKeyAndApplicationKey(org.key, application.key, MoveForm(orgKey = org2.key)))
    }.errors.map(_.message) must be(Seq(s"Organization[${org2.key}] already has an application[${application.key}]]"))
  }

}
