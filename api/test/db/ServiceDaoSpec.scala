package db

import com.gilt.apidoc.models.{Service, Visibility}
import org.scalatest.{FunSpec, Matchers}
import org.junit.Assert._
import java.util.UUID

class ServiceDaoSpec extends FunSpec with Matchers {
  new play.core.StaticApplication(new java.io.File("."))

  private lazy val name = "Service %s".format(UUID.randomUUID)
  private lazy val baseUrl = "http://localhost"

  private def upsertService(nameOption: Option[String] = None): Service = {
    val n = nameOption.getOrElse(name)
    ServiceDao.findByOrganizationAndName(Util.testOrg, n).getOrElse {
      val serviceForm = ServiceForm(
        name = n,
        description = None,
        visibility = Visibility.Organization
      )
      ServiceDao.create(Util.createdBy, Util.testOrg, serviceForm)
    }
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
      ServiceDao.findByOrganizationAndKey(Util.testOrg, service.key).get.name should be(newName)
    }

    it("description") {
      val service = upsertService()
      val newDescription = "Service %s".format(UUID.randomUUID)
      ServiceDao.findByOrganizationAndKey(Util.testOrg, service.key).get.description should be(None)
      ServiceDao.update(Util.createdBy, service.copy(description = Some(newDescription)))
      ServiceDao.findByOrganizationAndKey(Util.testOrg, service.key).get.description should be(Some(newDescription))
    }

    it("visibility") {
      val service = upsertService()
      service.visibility should be(Some(Visibility.Organization))

      ServiceDao.update(Util.createdBy, service.copy(visibility = Some(Visibility.Public)))
      ServiceDao.findByOrganizationAndKey(Util.testOrg, service.key).get.visibility should be(Some(Visibility.Public))

      ServiceDao.update(Util.createdBy, service.copy(visibility = Some(Visibility.Organization)))
      ServiceDao.findByOrganizationAndKey(Util.testOrg, service.key).get.visibility should be(Some(Visibility.Organization))
    }
  }

  it("findAll") {
    val name1 = "Service %s".format(UUID.randomUUID)
    val service1 = upsertService(Some(name1))

    val name2 = "Service %s".format(UUID.randomUUID)
    val service2 = upsertService(Some(name2))

    val names = ServiceDao.findAll(orgKey = Util.testOrg.key).map(_.name)
    assertTrue(names.contains(name1))
    assertTrue(names.contains(name2))
  }

  it("find by name") {
    val service = upsertService()
    assertEquals(service, ServiceDao.findByOrganizationAndName(Util.testOrg, name).get)
  }

  it("not create a new record if already exists") {
    val service1 = upsertService()
    val service2 = upsertService()
    assertEquals(service1, service2)
  }

}
