package db

import java.util.UUID

object Util {
  new play.core.StaticApplication(new java.io.File("."))

  def upsertUser(email: String): User = {
    UserDao.findByEmail(email).getOrElse {
      UserDao.create(UserForm(email = email, name = Some("Admin"), password = "test"))
    }
  }

  def upsertOrganization(name: String): Organization = {
    OrganizationDao.findAll(name = Some(name)).headOption.getOrElse {
      createOrganization(name = Some(name))
    }
  }

  def createOrganization(name: Option[String] = None): Organization = {
    val form = OrganizationForm(
      name = name.getOrElse(UUID.randomUUID().toString)
    )
    OrganizationDao.createWithAdministrator(Util.createdBy, form)
  }

  lazy val createdBy = Util.upsertUser("admin@apidoc.me")

  lazy val gilt = upsertOrganization("Gilt")

  lazy val testOrg = upsertOrganization("Test Org %s".format(UUID.randomUUID))

}
