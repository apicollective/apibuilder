package controllers

import io.apibuilder.api.v0.models.Visibility
import java.util.UUID

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class OrganizationsSpec extends PlaySpec with MockClient with OneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /organizations" in {
    val name = UUID.randomUUID.toString
    val org = createOrganization(name = Some(name))
    org.name must equal(name)
  }

  "POST /organizations trims whitespace in name" in {
    val name = UUID.randomUUID.toString
    val org = createOrganization(name = Some(" " + name + " "))
    org.name must equal(name)
  }

  "POST /organizations validates key is valid" in {
    expectErrors {
      client.organizations.post(createOrganizationForm(name = UUID.randomUUID.toString, key = Some("a")))
    }.errors.map(_.message) must equal(Seq(s"Key must be at least 3 characters"))

    expectErrors {
      client.organizations.post(createOrganizationForm(name = UUID.randomUUID.toString, key = Some("a bad key")))
    }.errors.map(_.message) must equal(Seq(s"Key must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid key would be: a-bad-key"))
  }

  "DELETE /organizations/:key" in {
    val org = createOrganization()
    await(client.organizations.deleteByKey(org.key)) must equal(())
    await(client.organizations.get(key = Some(org.key))) must equal(Nil)
  }

  "GET /organizations" in {
    val org1 = createOrganization()
    val org2 = createOrganization()

    await(client.organizations.get(key = Some(UUID.randomUUID.toString))) must equal(Nil)
    await(client.organizations.get(key = Some(org1.key))).head.guid must equal(org1.guid)
    await(client.organizations.get(key = Some(org2.key))).head.guid must equal(org2.guid)
  }

  "GET /organizations/:key" in {
    val org = createOrganization()
    await(client.organizations.getByKey(org.key)).guid must equal(org.guid)
    expectNotFound {
      client.organizations.getByKey(UUID.randomUUID.toString)
    }
  }

  "GET /organizations for an anonymous user shows only public orgs" in {
    val privateOrg = createOrganization(
      createOrganizationForm().copy(visibility = Visibility.Organization),
      createdBy
    )
    val publicOrg = createOrganization(
      createOrganizationForm().copy(visibility = Visibility.Public),
      createdBy
    )
    val anonymous = createUser()

    val client = newClient(anonymous)
    expectNotFound {
      client.organizations.getByKey(privateOrg.key)
    }
    await(client.organizations.getByKey(publicOrg.key)).key must equal(publicOrg.key)

    await(client.organizations.get(key = Some(privateOrg.key))) must equal(Nil)
    await(client.organizations.get(key = Some(publicOrg.key))).map(_.key) must equal(Seq(publicOrg.key))
  }

}
