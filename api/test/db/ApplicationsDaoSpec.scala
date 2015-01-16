package db

import com.gilt.apidoc.v0.models.{Application, ApplicationForm, Organization, Visibility}
import org.scalatest.{FunSpec, Matchers}
import org.junit.Assert._
import java.util.UUID

class ApplicationsDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  private lazy val baseUrl = "http://localhost"

  private def upsertApplication(nameOption: Option[String] = None): Application = {
    val n = nameOption.getOrElse("Application %s".format(UUID.randomUUID))
    ApplicationsDao.findAll(Authorization.All, orgKey = Some(Util.testOrg.key), name = Some(n), limit = 1).headOption.getOrElse {
      val applicationForm = ApplicationForm(
        name = n,
        description = None,
        visibility = Visibility.Organization
      )
      ApplicationsDao.create(Util.createdBy, Util.testOrg, applicationForm)
    }
  }

  private def findByKey(org: Organization, key: String): Option[Application] = {
    ApplicationsDao.findAll(Authorization.All, orgKey = Some(org.key), key = Some(key), limit = 1).headOption
  }

  it("create") {
    val name = "Application %s".format(UUID.randomUUID)
    val application = upsertApplication(Some(name))
    application.name should be(name)
  }

  describe("canUserUpdate") {

    it("for an org I belong to") {
      val user = Util.createRandomUser()
      val org = Util.createOrganization(user)
      val app = Util.createApplication(org)
      ApplicationsDao.canUserUpdate(user, app) should be(true)
    }

    it("for an org I do NOT belong to") {
      val app = Util.createApplication()
      ApplicationsDao.canUserUpdate(Util.createRandomUser(), app) should be(false)
    }

  }

  describe("validate") {

    def createForm() = ApplicationForm(
      name = "Application %s".format(UUID.randomUUID),
      key = None,
      description = None,
      visibility = Visibility.Organization
    )

    it("returns empty if valid") {
      ApplicationsDao.validate(Util.testOrg, createForm(), None) should be(Seq.empty)
    }

    it("returns error if name already exists") {
      val form = createForm()
      ApplicationsDao.create(Util.createdBy, Util.testOrg, form)
      ApplicationsDao.validate(Util.testOrg, form, None).map(_.code) should be(Seq("validation_error"))
    }

    it("returns empty if name exists but belongs to the application we are updating") {
      val form = createForm()
      val application = ApplicationsDao.create(Util.createdBy, Util.testOrg, form)
      ApplicationsDao.validate(Util.testOrg, form, Some(application)) should be(Seq.empty)
    }

    it("key") {
      val form = createForm()
      val application = ApplicationsDao.create(Util.createdBy, Util.testOrg, form)

      val newForm = form.copy(name = application.name + "2", key = Some(application.key))
      ApplicationsDao.validate(Util.testOrg, newForm, None).map(_.message) should be(Seq("Application with this key already exists"))
      ApplicationsDao.validate(Util.testOrg, newForm, Some(application)) should be(Seq.empty)
    }

  }

  describe("update") {

    def toForm(app: Application): ApplicationForm = {
      ApplicationForm(
        name = app.name,
        key = Some(app.key),
        description = app.description,
        visibility = app.visibility
      )
    }

    it("name") {
      val name = "Application %s".format(UUID.randomUUID)
      val application = upsertApplication(Some(name))
      val newName = application.name + "2"
      ApplicationsDao.update(Util.createdBy, application, toForm(application).copy(name = newName))
      findByKey(Util.testOrg, application.key).get.name should be(newName)
    }

    it("description") {
      val application = upsertApplication()
      val newDescription = "Application %s".format(UUID.randomUUID)
      findByKey(Util.testOrg, application.key).get.description should be(None)
      ApplicationsDao.update(Util.createdBy, application, toForm(application).copy(description = Some(newDescription)))
      findByKey(Util.testOrg, application.key).get.description should be(Some(newDescription))
    }

    it("visibility") {
      val application = upsertApplication()
      application.visibility should be(Visibility.Organization)

      ApplicationsDao.update(Util.createdBy, application, toForm(application).copy(visibility = Visibility.Public))
      findByKey(Util.testOrg, application.key).get.visibility should be(Visibility.Public)

      ApplicationsDao.update(Util.createdBy, application, toForm(application).copy(visibility = Visibility.Organization))
      findByKey(Util.testOrg, application.key).get.visibility should be(Visibility.Organization)
    }
  }

  describe("findAll") {

    val user = Util.createRandomUser()
    val org = Util.createOrganization(user, Some("Public " + UUID.randomUUID().toString))
    val publicApplication = ApplicationsDao.create(user, org, ApplicationForm(name = "svc-public", visibility = Visibility.Public))
    val privateApplication = ApplicationsDao.create(user, org, ApplicationForm(name = "svc-private", visibility = Visibility.Organization))

    it("by orgKey") {
      val guids = ApplicationsDao.findAll(Authorization.All, orgKey = Some(org.key)).map(_.guid)
      guids.contains(publicApplication.guid) should be(true)
      guids.contains(privateApplication.guid) should be(true)
    }

    it("by guid") {
      val guids = ApplicationsDao.findAll(Authorization.All, orgKey = Some(org.key), guid = Some(publicApplication.guid)).map(_.guid)
      guids.contains(publicApplication.guid) should be(true)
      guids.contains(privateApplication.guid) should be(false)
    }

    it("by key") {
      val guids = ApplicationsDao.findAll(Authorization.All, orgKey = Some(org.key), key = Some(publicApplication.key)).map(_.guid)
      guids.contains(publicApplication.guid) should be(true)
      guids.contains(privateApplication.guid) should be(false)
    }

    it("by name") {
      val guids = ApplicationsDao.findAll(Authorization.All, orgKey = Some(org.key), name = Some(publicApplication.name)).map(_.guid)
      guids.contains(publicApplication.guid) should be(true)
      guids.contains(privateApplication.guid) should be(false)
    }

    describe("Authorization") {

      describe("All") {

        it("sees both applications") {
          val guids = ApplicationsDao.findAll(Authorization.All, orgKey = Some(org.key)).map(_.guid)
          guids.contains(publicApplication.guid) should be(true)
          guids.contains(privateApplication.guid) should be(true)
        }

      }

      describe("PublicOnly") {

        it("sees only the public application") {
          val guids = ApplicationsDao.findAll(Authorization.PublicOnly, orgKey = Some(org.key)).map(_.guid)
          guids.contains(publicApplication.guid) should be(true)
          guids.contains(privateApplication.guid) should be(false)
        }

      }

      describe("User") {

        it("user can see own application") {
          val guids = ApplicationsDao.findAll(Authorization.User(user.guid), orgKey = Some(org.key)).map(_.guid)
          guids.contains(publicApplication.guid) should be(true)
          guids.contains(privateApplication.guid) should be(true)
        }

        it("other user cannot see private application") {
          val guids = ApplicationsDao.findAll(Authorization.User(Util.createdBy.guid), orgKey = Some(org.key)).map(_.guid)
          guids.contains(publicApplication.guid) should be(true)
          guids.contains(privateApplication.guid) should be(false)
        }
      }

    }

  }

}
