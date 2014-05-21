package db

import java.util.UUID

object Util {
  new play.core.StaticApplication(new java.io.File("."))

  lazy val createdBy = UserDao.upsert("admin@apidoc.me")
  lazy val gilt = OrganizationDao.findAll(name = Some("gilt")).headOption.getOrElse {
    OrganizationDao.createWithAdministrator(createdBy, "Gilt")
  }

  private lazy val testOrgName = "Test Org %s".format(UUID.randomUUID)
  lazy val testOrg = OrganizationDao.findAll(name = Some(testOrgName)).headOption.getOrElse {
    OrganizationDao.createWithAdministrator(createdBy, testOrgName)
  }

}
