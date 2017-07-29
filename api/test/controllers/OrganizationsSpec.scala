package controllers

import db.OrganizationsDao
import io.apibuilder.api.v0.models.{Organization, OrganizationForm, Visibility}
import io.apibuilder.api.v0.errors.{ErrorsResponse, UnitResponse}
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._

class OrganizationsSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /organizations" in new WithServer {
    val name = UUID.randomUUID.toString
    val org = createOrganization(createOrganizationForm(name = name))
    org.name must be(name)
  }

  "POST /organizations trims whitespace in name" in new WithServer {
    val name = UUID.randomUUID.toString
    val org = createOrganization(createOrganizationForm(name = " " + name + " "))
    org.name must be(name)
  }

  "POST /organizations validates key is valid" in new WithServer {
    intercept[ErrorsResponse] {
      createOrganization(createOrganizationForm(name = UUID.randomUUID.toString, key = Some("a")))
    }.errors.map(_.message) must be(Seq(s"Key must be at least 3 characters"))

    intercept[ErrorsResponse] {
      createOrganization(createOrganizationForm(name = UUID.randomUUID.toString, key = Some("a bad key")))
    }.errors.map(_.message) must be(Seq(s"Key must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid key would be: a-bad-key"))
  }

  "DELETE /organizations/:key" in new WithServer {
    val org = createOrganization()
    await(client.organizations.deleteByKey(org.key)) must be(())
    await(client.organizations.get(key = Some(org.key))) must be(Nil)
  }

  "GET /organizations" in new WithServer {
    val org1 = createOrganization()
    val org2 = createOrganization()

    await(client.organizations.get(key = Some(UUID.randomUUID.toString))) must be(Nil)
    await(client.organizations.get(key = Some(org1.key))).head.guid must be(org1.guid)
    await(client.organizations.get(key = Some(org2.key))).head.guid must be(org2.guid)
  }

  "GET /organizations/:key" in new WithServer {
    val org = createOrganization()
    await(client.organizations.getByKey(org.key)).guid must be(org.guid)
    intercept[UnitResponse] {
      await(client.organizations.getByKey(UUID.randomUUID.toString))
    }.status must be(404)
  }

  "GET /organizations for an anonymous user shows only public orgs" in new WithServer {
    val privateOrg = createOrganization(createOrganizationForm().copy(visibility = Visibility.Organization))
    val publicOrg = createOrganization(createOrganizationForm().copy(visibility = Visibility.Public))
    val anonymous = createUser()

    val client = newClient(anonymous)
    intercept[UnitResponse] {
      await(client.organizations.getByKey(privateOrg.key))
    }.status must be(404)
    await(client.organizations.getByKey(publicOrg.key)).key must be(publicOrg.key)

    await(client.organizations.get(key = Some(privateOrg.key))) must be(Nil)
    await(client.organizations.get(key = Some(publicOrg.key))).map(_.key) must be(Seq(publicOrg.key))
  }

}
