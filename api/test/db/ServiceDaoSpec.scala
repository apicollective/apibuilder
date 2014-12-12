package db

import com.gilt.apidoc.models.{Organization, Service, Visibility}
import org.scalatest.{FunSpec, Matchers}
import org.junit.Assert._
import java.util.UUID

class ServiceDaoSpec extends FunSpec with Matchers {
  new play.core.StaticApplication(new java.io.File("."))

  private lazy val baseUrl = "http://localhost"

  private def upsertService(nameOption: Option[String] = None): Service = {
    val n = nameOption.getOrElse("Service %s".format(UUID.randomUUID))
    ServiceDao.findAll(Authorization.All, orgKey = Some(Util.testOrg.key), name = Some(n), limit = 1).headOption.getOrElse {
      val serviceForm = ServiceForm(
        name = n,
        description = None,
        visibility = Visibility.Organization
      )
      ServiceDao.create(Util.createdBy, Util.testOrg, serviceForm)
    }
  }

  private def findByKey(org: Organization, key: String): Option[Service] = {
    ServiceDao.findAll(Authorization.All, orgKey = Some(org.key), key = Some(key), limit = 1).headOption
  }

  it("create") {
    val name = "Service %s".format(UUID.randomUUID)
    val service = upsertService(Some(name))
    service.name should be(name)
  }

  describe("validate") {

    def createForm() = ServiceForm(
      name = "Service %s".format(UUID.randomUUID),
      description = None,
      visibility = Visibility.Organization
    )

    it("returns empty if valid") {
      ServiceDao.validate(Util.testOrg, createForm(), None) should be(Seq.empty)
    }

    it("returns error if name already exists") {
      val form = createForm()
      ServiceDao.create(Util.createdBy, Util.testOrg, form)
      ServiceDao.validate(Util.testOrg, form, None).map(_.code) should be(Seq("validation_error"))
    }

    it("returns empty if name exists but belongs to the service we are updating") {
      val form = createForm()
      val service = ServiceDao.create(Util.createdBy, Util.testOrg, form)
      ServiceDao.validate(Util.testOrg, form, Some(service)) should be(Seq.empty)
    }

  }

  describe("update") {

    it("name") {
      val name = "Service %s".format(UUID.randomUUID)
      val service = upsertService(Some(name))
      val newName = service.name + "2"
      ServiceDao.update(Util.createdBy, service.copy(name = newName))
      findByKey(Util.testOrg, service.key).get.name should be(newName)
    }

    it("description") {
      val service = upsertService()
      val newDescription = "Service %s".format(UUID.randomUUID)
      findByKey(Util.testOrg, service.key).get.description should be(None)
      ServiceDao.update(Util.createdBy, service.copy(description = Some(newDescription)))
      findByKey(Util.testOrg, service.key).get.description should be(Some(newDescription))
    }

    it("visibility") {
      val service = upsertService()
      service.visibility should be(Visibility.Organization)

      ServiceDao.update(Util.createdBy, service.copy(visibility = Visibility.Public))
      findByKey(Util.testOrg, service.key).get.visibility should be(Visibility.Public)

      ServiceDao.update(Util.createdBy, service.copy(visibility = Visibility.Organization))
      findByKey(Util.testOrg, service.key).get.visibility should be(Visibility.Organization)
    }
  }

  describe("findAll") {

    val user = Util.createRandomUser()
    val org = Util.createOrganization(user, Some("Public " + UUID.randomUUID().toString))
    val publicService = ServiceDao.create(user, org, ServiceForm(name = "svc-public", visibility = Visibility.Public))
    val privateService = ServiceDao.create(user, org, ServiceForm(name = "svc-private", visibility = Visibility.Organization))

    it("by orgKey") {
      val guids = ServiceDao.findAll(Authorization.All, orgKey = Some(org.key)).map(_.guid)
      guids.contains(publicService.guid) should be(true)
      guids.contains(privateService.guid) should be(true)
    }

    it("by guid") {
      val guids = ServiceDao.findAll(Authorization.All, orgKey = Some(org.key), guid = Some(publicService.guid)).map(_.guid)
      guids.contains(publicService.guid) should be(true)
      guids.contains(privateService.guid) should be(false)
    }

    it("by key") {
      val guids = ServiceDao.findAll(Authorization.All, orgKey = Some(org.key), key = Some(publicService.key)).map(_.guid)
      guids.contains(publicService.guid) should be(true)
      guids.contains(privateService.guid) should be(false)
    }

    it("by name") {
      val guids = ServiceDao.findAll(Authorization.All, orgKey = Some(org.key), name = Some(publicService.name)).map(_.guid)
      guids.contains(publicService.guid) should be(true)
      guids.contains(privateService.guid) should be(false)
    }

    describe("Authorization") {

      describe("All") {

        it("sees both services") {
          val guids = ServiceDao.findAll(Authorization.All, orgKey = Some(org.key)).map(_.guid)
          guids.contains(publicService.guid) should be(true)
          guids.contains(privateService.guid) should be(true)
        }

      }

      describe("PublicOnly") {

        it("sees only the public service") {
          val guids = ServiceDao.findAll(Authorization.PublicOnly, orgKey = Some(org.key)).map(_.guid)
          guids.contains(publicService.guid) should be(true)
          guids.contains(privateService.guid) should be(false)
        }

      }

      describe("User") {

        it("user can see own service") {
          val guids = ServiceDao.findAll(Authorization.User(user.guid), orgKey = Some(org.key)).map(_.guid)
          guids.contains(publicService.guid) should be(true)
          guids.contains(privateService.guid) should be(true)
        }

        it("other user cannot see private service") {
          val guids = ServiceDao.findAll(Authorization.User(Util.createdBy.guid), orgKey = Some(org.key)).map(_.guid)
          guids.contains(publicService.guid) should be(true)
          guids.contains(privateService.guid) should be(false)
        }
      }

    }

  }

}
