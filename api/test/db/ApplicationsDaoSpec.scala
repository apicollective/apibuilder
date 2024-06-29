package db

import io.apibuilder.api.v0.models._
import io.apibuilder.common.v0.models.MembershipRole
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json

import java.util.UUID

class ApplicationsDaoSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers {

  private val Original = io.apibuilder.api.v0.models.Original(
    `type` = OriginalType.ApiJson,
    data = Json.obj(
      "name" -> s"test-${UUID.randomUUID}"
    ).toString
  )

  private def upsertApplication(
    nameOption: Option[String] = None,
    org: Organization = testOrg,
    visibility: Visibility = Visibility.Organization
  ): Application = {
    val n = nameOption.getOrElse("Test %s".format(UUID.randomUUID))
    applicationsDao.findAll(Authorization.All, orgKey = Some(org.key), name = Some(n), limit = 1).headOption.getOrElse {
      val applicationForm = ApplicationForm(
        name = n,
        description = None,
        visibility = visibility
      )
      applicationsDao.create(testUser, org, applicationForm)
    }
  }

  private def findByKey(org: Organization, key: String): Option[Application] = {
    applicationsDao.findAll(Authorization.All, orgKey = Some(org.key), key = Some(key), limit = 1).headOption
  }

  "create" in {
    val name = "Test %s".format(UUID.randomUUID)
    val application = upsertApplication(Some(name))
    application.name must be(name)
  }

  "canUserUpdate" must {

    "for an org I belong to" in {
      val user = createRandomUser()
      val org = createOrganization(user)
      val app = createApplication(org)
      applicationsDao.canUserUpdate(user, app) must be(true)
    }

    "for an org I do NOT belong to" in {
      val app = createApplication()
      applicationsDao.canUserUpdate(createRandomUser(), app) must be(false)
    }

  }

  "validate" must {

    def createForm() = ApplicationForm(
      name = "Test %s".format(UUID.randomUUID),
      key = None,
      description = None,
      visibility = Visibility.Organization
    )

    "returns empty if valid" in {
      applicationsDao.validate(testOrg, createForm(), None) must be(Nil)
    }

    "returns error if name already exists" in {
      val form = createForm()
      applicationsDao.create(testUser, testOrg, form)
      applicationsDao.validate(testOrg, form, None).map(_.code) must be(Seq("validation_error"))
    }

    "returns empty if name exists but belongs to the application we are updating" in {
      val form = createForm()
      val application = applicationsDao.create(testUser, testOrg, form)
      applicationsDao.validate(testOrg, form, Some(application)) must be(Nil)
    }

    "key" in {
      val form = createForm()
      val application = applicationsDao.create(testUser, testOrg, form)

      val newForm = form.copy(name = application.name + "2", key = Some(application.key))
      applicationsDao.validate(testOrg, newForm, None).map(_.message) must be(Seq("Application with this key already exists"))
      applicationsDao.validate(testOrg, newForm, Some(application)) must be(Nil)
    }

  }

  "update" must {

    def toForm(app: Application): ApplicationForm = {
      ApplicationForm(
        name = app.name,
        key = Some(app.key),
        description = app.description,
        visibility = app.visibility
      )
    }

    "name" in {
      val name = "Test %s".format(UUID.randomUUID)
      val application = upsertApplication(Some(name))
      val newName = application.name + "2"
      applicationsDao.update(testUser, application, toForm(application).copy(name = newName))
      findByKey(testOrg, application.key).get.name must be(newName)
    }

    "description" in {
      val application = upsertApplication()
      val newDescription = "Test %s".format(UUID.randomUUID)
      findByKey(testOrg, application.key).get.description must be(None)
      applicationsDao.update(testUser, application, toForm(application).copy(description = Some(newDescription)))
      findByKey(testOrg, application.key).get.description must be(Some(newDescription))
    }

    "visibility" in {
      val application = upsertApplication()
      application.visibility must be(Visibility.Organization)

      applicationsDao.update(testUser, application, toForm(application).copy(visibility = Visibility.Public))
      findByKey(testOrg, application.key).get.visibility must be(Visibility.Public)

      applicationsDao.update(testUser, application, toForm(application).copy(visibility = Visibility.Organization))
      findByKey(testOrg, application.key).get.visibility must be(Visibility.Organization)
    }
  }

  "findAll" must {

    lazy val user = createRandomUser()
    lazy val org = createOrganization(user, Some("Public " + UUID.randomUUID().toString))
    lazy val publicApplication = applicationsDao.create(user, org, ApplicationForm(name = "svc-public", visibility = Visibility.Public))
    lazy val privateApplication = applicationsDao.create(user, org, ApplicationForm(name = "svc-private", visibility = Visibility.Organization))

    def initialize(): Unit = {
      publicApplication
      privateApplication
    }

    "by version" in {
      val v1 = createVersion(publicApplication)
      val v2 = createVersion(publicApplication)
      val privateVersion = createVersion(privateApplication)

      def findByVersion(version: Version) = {
        applicationsDao.findAll(Authorization.All, version = Some(version)).map(_.guid)
      }

      findByVersion(v1) mustBe Seq(publicApplication.guid)
      findByVersion(v2) mustBe Seq(publicApplication.guid)
      findByVersion(privateVersion) mustBe Seq(privateApplication.guid)
    }

    "by orgKey" in {
      initialize()
      val guids = applicationsDao.findAll(Authorization.All, orgKey = Some(org.key)).map(_.guid)
      guids.contains(publicApplication.guid) must be(true)
      guids.contains(privateApplication.guid) must be(true)
    }

    "by guid" in {
      initialize()
      val guids = applicationsDao.findAll(Authorization.All, orgKey = Some(org.key), guid = Some(publicApplication.guid)).map(_.guid)
      guids.contains(publicApplication.guid) must be(true)
      guids.contains(privateApplication.guid) must be(false)
    }

    "by key" in {
      initialize()
      val guids = applicationsDao.findAll(Authorization.All, orgKey = Some(org.key), key = Some(publicApplication.key)).map(_.guid)
      guids.contains(publicApplication.guid) must be(true)
      guids.contains(privateApplication.guid) must be(false)
    }

    "by name" in {
      initialize()
      val guids = applicationsDao.findAll(Authorization.All, orgKey = Some(org.key), name = Some(publicApplication.name)).map(_.guid)
      guids.contains(publicApplication.guid) must be(true)
      guids.contains(privateApplication.guid) must be(false)
    }

    "hasVersion" in {
      initialize()
      val app = createApplication(org)

      applicationsDao.findAll(
        Authorization.All,
        guid = Some(app.guid),
        hasVersion = Some(false)
      ).map(_.guid) must be(Seq(app.guid))

      applicationsDao.findAll(
        Authorization.All,
        guid = Some(app.guid),
        hasVersion = Some(true)
      ).map(_.guid) must be(Nil)

      val service = createService(app)
      versionsDao.create(testUser, app, "1.0.0", Original, service)

      applicationsDao.findAll(
        Authorization.All,
        guid = Some(app.guid),
        hasVersion = Some(false)
      ).map(_.guid) must be(Nil)

      applicationsDao.findAll(
        Authorization.All,
        guid = Some(app.guid),
        hasVersion = Some(true)
      ).map(_.guid) must be(Seq(app.guid))

    }

    "Authorization" must {

      "All" must {

        "sees both applications" in {
          initialize()
          val guids = applicationsDao.findAll(Authorization.All, orgKey = Some(org.key)).map(_.guid)
          guids.contains(publicApplication.guid) must be(true)
          guids.contains(privateApplication.guid) must be(true)
        }

      }

      "PublicOnly" must {

        "sees only the public application" in {
          initialize()
          val guids = applicationsDao.findAll(Authorization.PublicOnly, orgKey = Some(org.key)).map(_.guid)
          guids.contains(publicApplication.guid) must be(true)
          guids.contains(privateApplication.guid) must be(false)
        }

      }

      "User" must {

        "user can see own application" in {
          initialize()
          val guids = applicationsDao.findAll(Authorization.User(user.guid), orgKey = Some(org.key)).map(_.guid)
          guids.contains(publicApplication.guid) must be(true)
          guids.contains(privateApplication.guid) must be(true)
        }

        "other user cannot see public nor private applications for a private org" in {
          initialize()
          val guids = applicationsDao.findAll(Authorization.User(testUser.guid), orgKey = Some(org.key)).map(_.guid)
          guids.contains(publicApplication.guid) must be(false)
          guids.contains(privateApplication.guid) must be(false)
        }

        "other user cannot see private application even for a public org" in {
          initialize()
          val myUser = createRandomUser()
          val myOrg = createOrganization(visibility = Visibility.Public)
          createMembership(myOrg, myUser, MembershipRole.Member)
          val myPrivateApp = upsertApplication(org = myOrg)
          val myPublicApp = upsertApplication(org = myOrg, visibility = Visibility.Public)

          val otherUser = createRandomUser()
          val otherOrg = createOrganization(visibility = Visibility.Public)
          createMembership(otherOrg, otherUser, MembershipRole.Member)
          val otherPrivateApp = upsertApplication(org = otherOrg)
          val otherPublicApp = upsertApplication(org = otherOrg, visibility = Visibility.Public)

          val myGuids = applicationsDao.findAll(Authorization.User(myUser.guid), orgKey = Some(myOrg.key)).map(_.guid)
          myGuids.contains(myPrivateApp.guid) must be(true)
          myGuids.contains(myPublicApp.guid) must be(true)

          val otherGuids = applicationsDao.findAll(Authorization.User(myUser.guid), orgKey = Some(otherOrg.key)).map(_.guid)
          otherGuids.contains(otherPrivateApp.guid) must be(false)
          otherGuids.contains(otherPublicApp.guid) must be(true)
        }
      }

    }

  }
}
