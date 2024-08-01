package controllers

import db.InternalOrganization
import io.apibuilder.api.v0.models.{Application, MoveForm, Organization}

import java.util.UUID
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec

class ApplicationsSpec extends PlaySpec with MockClient with GuiceOneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  private def getByKey(org: InternalOrganization, key: String): Option[Application] = {
    await(client.applications.get(org.key, key = Some(key), limit = 1)).headOption
  }

  private lazy val org = createOrganization(createdBy = testUser)

  "POST /:orgKey" in {
    val key = "test-" + UUID.randomUUID.toString
    val form = createApplicationForm(key = Some(key))
    createApplication(org, form).key must equal(key)
  }

  "POST /:orgKey trims whitespace in name" in {
    val name = UUID.randomUUID.toString
    createApplication(org, createApplicationForm(name = " " + name + " ")).name must equal(name)
  }

  "POST /:orgKey validates key is valid" in {
    expectErrors {
      client.applications.post(org.key, createApplicationForm(name = UUID.randomUUID.toString, key = Some("a")))
    }.errors.map(_.message) must equal(Seq(s"Key must be at least 3 characters"))

    expectErrors {
      client.applications.post(org.key, createApplicationForm(name = UUID.randomUUID.toString, key = Some("a bad key")))
    }.errors.map(_.message) must equal(Seq(s"Key must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid key would be: a-bad-key"))
  }

  "DELETE /:org/:key" in {
    val application = createApplication(org)
    await(client.applications.deleteByApplicationKey(org.key, application.key)) must equal(())
    getByKey(org, application.key) must be(None)
  }

  "GET /:orgKey by application name" in {
    val app1 = createApplication(org)
    val app2 = createApplication(org)

    await(client.applications.get(org.key, name = Some(UUID.randomUUID.toString))) must equal(Nil)
    await(client.applications.get(org.key, name = Some(app1.name))).map(_.guid) must equal(Seq(app1.guid))
    await(client.applications.get(org.key, name = Some(app2.name.toUpperCase))).map(_.guid) must equal(Seq(app2.guid))
  }

  "GET /:orgKey by application key" in {
    val app1 = createApplication(org)
    val app2 = createApplication(org)

    await(client.applications.get(org.key, key = Some(UUID.randomUUID.toString))) must equal(Nil)
    await(client.applications.get(org.key, key = Some(app1.key))).map(_.guid) must equal(Seq(app1.guid))
    await(client.applications.get(org.key, key = Some(app2.key.toUpperCase))).map(_.guid) must equal(Seq(app2.guid))
  }

  "GET /:orgKey by hasVersion" in {
    val org = createOrganization()
    val app1 = createApplication(org)
    val app2 = createApplication(org)
    createVersion(app2)

    await(client.applications.get(org.key, hasVersion = None)).map(_.key).sorted must equal(Seq(app2.key, app1.key).sorted)
    await(client.applications.get(org.key, hasVersion = Some(false))).map(_.key) must equal(Seq(app1.key))
    await(client.applications.get(org.key, hasVersion = Some(true))).map(_.key) must equal(Seq(app2.key))
  }

  "POST /:orgKey/:applicationKey/move" in {
    val org2 = createOrganization()
    val application = createApplication(org)

    val updated = await(client.applications.postMoveByApplicationKey(org.key, application.key, MoveForm(orgKey = org2.key)))
    updated.organization.guid must equal(org2.guid)
  }

  "POST /:orgKey/:applicationKey/move validates org key" in {
    val application = createApplication(org)
    val key = UUID.randomUUID.toString

    expectErrors {
      client.applications.postMoveByApplicationKey(org.key, application.key, MoveForm(orgKey = key))
    }.errors.map(_.message) must equal(Seq(s"Organization[$key] not found"))
  }

  "POST /:orgKey/:applicationKey/move validates duplicate application key" in {
    val application = createApplication(org)
    val org2 = createOrganization()
    createApplication(org2, createApplicationForm().copy(key = Some(application.key)))

    // Test no-op if moving own app
    await(client.applications.postMoveByApplicationKey(org.key, application.key, MoveForm(orgKey = org.key)))

    // Test validation if org key already defined for the org to which we are moving the app
    expectErrors {
      client.applications.postMoveByApplicationKey(org.key, application.key, MoveForm(orgKey = org2.key))
    }.errors.map(_.message) must equal(Seq(s"Organization[${org2.key}] already has an application[${application.key}]]"))
  }

}
