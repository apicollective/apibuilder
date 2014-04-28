package db

import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID

class ServiceDaoSpec extends FlatSpec {
  new play.core.StaticApplication(new java.io.File("."))

  private lazy val name = "Service %s".format(UUID.randomUUID)
  private lazy val baseUrl = "http://localhost"

  private def upsertService(nameOption: Option[String] = None): Service = {
    val n = nameOption.getOrElse(name)
    ServiceDao.findByOrganizationAndName(Util.testOrg, n).getOrElse {
      ServiceDao.create(Util.createdBy, Util.testOrg, n)
    }
  }

  it should "create" in {
    val service = upsertService()
  }

  it should "findAll" in {
    val service = upsertService()

    val name2 = "Service %s".format(UUID.randomUUID)
    val service2 = upsertService(Some(name2))
    val names = ServiceDao.findAll(orgKey = Util.testOrg.key).map(_.name)
    assertTrue(names.contains(name))
    assertTrue(names.contains(name2))
  }

  it should "find by name" in {
    val service = upsertService()
    assertEquals(service, ServiceDao.findByOrganizationAndName(Util.testOrg, name).get)
  }

  it should "not create a new record if already exists" in {
    val service1 = upsertService()
    val service2 = upsertService()
    assertEquals(service1, service2)
  }

}
