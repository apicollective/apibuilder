package controllers

import db.OrganizationsDao
import com.gilt.apidoc.v0.models.{Organization, OrganizationForm, Visibility}
import com.gilt.apidoc.v0.errors.ErrorsResponse
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
    }.errors.map(_.message) must be(Seq(s"Key must be at least ${db.OrganizationsDao.MinKeyLength} characters"))

    intercept[ErrorsResponse] {
      createOrganization(createOrganizationForm(name = UUID.randomUUID.toString, key = Some("a bad key")))
    }.errors.map(_.message) must be(Seq(s"Key must be in all lower case and contain alphanumerics only. A valid key would be: a-bad-key"))
  }

  "POST /organizations validates key is not reserved" in new WithServer {
    intercept[ErrorsResponse] {
      createOrganization(createOrganizationForm(name = "members"))
    }.errors.map(_.message) must be(Seq(s"Prefix member is a reserved word and cannot be used for the key of an organization"))
  }

  "POST /organizations validates key is not reserved when just a prefix" in new WithServer {
    intercept[ErrorsResponse] {
      createOrganization(createOrganizationForm(name = "membership_request"))
    }.errors.map(_.message) must be(Seq(s"Prefix member is a reserved word and cannot be used for the key of an organization"))
  }

  "DELETE /organizations/:key" in new WithServer {
    val org = createOrganization()
    await(client.organizations.deleteByKey(org.key)) must be(Some(()))
    await(client.organizations.get(key = Some(org.key))) must be(Seq.empty)
  }

  "GET /organizations" in new WithServer {
    val org1 = createOrganization()
    val org2 = createOrganization()

    await(client.organizations.get(key = Some(UUID.randomUUID.toString))) must be(Seq.empty)
    await(client.organizations.get(key = Some(org1.key))).head must be(org1)
    await(client.organizations.get(key = Some(org2.key))).head must be(org2)
  }

  "GET /organizations/:key" in new WithServer {
    val org = createOrganization()
    await(client.organizations.getByKey(org.key)) must be(Some(org))
    await(client.organizations.getByKey(UUID.randomUUID.toString)) must be(None)
  }

  "GET /organizations for an anonymous user shows only public orgs" in new WithServer {
    val privateOrg = createOrganization(createOrganizationForm().copy(visibility = Some(Visibility.Organization)))
    val publicOrg = createOrganization(createOrganizationForm().copy(visibility = Some(Visibility.Public)))
    val anonymous = createUser()

    val client = newClient(anonymous)
    await(client.organizations.getByKey(privateOrg.key)) must be(None)
    await(client.organizations.getByKey(publicOrg.key)).map(_.key) must be(Some(publicOrg.key))

    await(client.organizations.get(key = Some(privateOrg.key))) must be(Seq.empty)
    await(client.organizations.get(key = Some(publicOrg.key))).map(_.key) must be(Seq(publicOrg.key))
  }

}
