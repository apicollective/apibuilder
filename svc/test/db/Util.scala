package db

import java.util.UUID

object Util {
  new play.core.StaticApplication(new java.io.File("."))

  lazy val createdBy = UserDao.upsert("admin@apidoc.com")
  lazy val gilt = OrganizationDao.findByUserAndName(createdBy, "gilt").getOrElse {
    OrganizationDao.createWithAdministrator(createdBy, "Gilt")
  }

  private lazy val testOrgName = "Test Org %s".format(UUID.randomUUID)
  lazy val testOrg = OrganizationDao.findByUserAndName(createdBy, testOrgName).getOrElse {
    OrganizationDao.createWithAdministrator(createdBy, testOrgName)
  }

}
