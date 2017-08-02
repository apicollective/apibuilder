package controllers

import io.apibuilder.api.v0.models.Visibility
import java.util.UUID

import play.api.test._

class OrganizationsSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /organizations" in new WithServer(port=defaultPort) {
    val name = UUID.randomUUID.toString
    val org = createOrganization(createOrganizationForm(name = name))
    org.name must beEqualTo(name)
  }

  "POST /organizations trims whitespace in name" in new WithServer(port=defaultPort) {
    val name = UUID.randomUUID.toString
    val org = createOrganization(createOrganizationForm(name = " " + name + " "))
    org.name must beEqualTo(name)
  }

  "POST /organizations validates key is valid" in new WithServer(port=defaultPort) {
    expectErrors {
      client.organizations.post(createOrganizationForm(name = UUID.randomUUID.toString, key = Some("a")))
    }.errors.map(_.message) must beEqualTo(Seq(s"Key must be at least 3 characters"))

    expectErrors {
      client.organizations.post(createOrganizationForm(name = UUID.randomUUID.toString, key = Some("a bad key")))
    }.errors.map(_.message) must beEqualTo(Seq(s"Key must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid key would be: a-bad-key"))
  }

  "DELETE /organizations/:key" in new WithServer(port=defaultPort) {
    val org = createOrganization()
    await(client.organizations.deleteByKey(org.key)) must beEqualTo(())
    await(client.organizations.get(key = Some(org.key))) must beEqualTo(Nil)
  }

  "GET /organizations" in new WithServer(port=defaultPort) {
    val org1 = createOrganization()
    val org2 = createOrganization()

    await(client.organizations.get(key = Some(UUID.randomUUID.toString))) must beEqualTo(Nil)
    await(client.organizations.get(key = Some(org1.key))).head.guid must beEqualTo(org1.guid)
    await(client.organizations.get(key = Some(org2.key))).head.guid must beEqualTo(org2.guid)
  }

  "GET /organizations/:key" in new WithServer(port=defaultPort) {
    val org = createOrganization()
    await(client.organizations.getByKey(org.key)).guid must beEqualTo(org.guid)
    expectNotFound {
      client.organizations.getByKey(UUID.randomUUID.toString)
    }
  }

  "GET /organizations for an anonymous user shows only public orgs" in new WithServer(port=defaultPort) {
    val privateOrg = createOrganization(createOrganizationForm().copy(visibility = Visibility.Organization))
    val publicOrg = createOrganization(createOrganizationForm().copy(visibility = Visibility.Public))
    val anonymous = createUser()

    val client = newClient(anonymous)
    expectNotFound {
      client.organizations.getByKey(privateOrg.key)
    }
    await(client.organizations.getByKey(publicOrg.key)).key must beEqualTo(publicOrg.key)

    await(client.organizations.get(key = Some(privateOrg.key))) must beEqualTo(Nil)
    await(client.organizations.get(key = Some(publicOrg.key))).map(_.key) must beEqualTo(Seq(publicOrg.key))
  }

}
