package db

import java.util.UUID

object Util {
  new play.core.StaticApplication(new java.io.File("."))

  lazy val createdBy = User.upsert("otto@gilt.com")
  lazy val gilt = Organization.findByKey("gilt").getOrElse {
    Organization.create(createdBy, "Gilt")
  }

  private lazy val testOrgName = "Test Org %s".format(UUID.randomUUID)
  lazy val testOrg = Organization.findByKey(testOrgName).getOrElse {
    Organization.create(createdBy, testOrgName)
  }

}
