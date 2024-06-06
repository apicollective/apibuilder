package db

import java.util.UUID

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class VersionValidatorSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers {

  private[this] def versionValidator = injector.instanceOf[VersionValidator]
  
  "validates user is a member of the organization to create an application" in {
    val org = createOrganization()

    versionValidator.validate(createRandomUser(), org, UUID.randomUUID.toString) must be(
      Seq("You must be a member of this organization to update applications")
    )
  }

  "key" must {

    "validates if already exists" in {
      val org = createOrganization()
      val existing = createApplication(org)

      versionValidator.validate(testUser, org, existing.key) must be(
        Seq(s"An application with key[${existing.key}] already exists")
      )
    }

    "no errors for a new key" in {
      val org = createOrganization()
      createApplication(org)

      versionValidator.validate(testUser, org, UUID.randomUUID.toString) must be(Nil)
    }

    "no errors when updating an application with the same key" in {
      val org = createOrganization()
      val existing = createApplication(org)

      versionValidator.validate(testUser, org, existing.key, Some(existing.key)) must be(Nil)
    }

    "validates that key is not changing" in {
      val org = createOrganization()
      val existing = createApplication(org)

      val newKey = UUID.randomUUID.toString
      versionValidator.validate(testUser, org, newKey, Some(existing.key)) must be(
        Seq(s"The application key[$newKey] in the uploaded file does not match the existing application key[${existing.key}]. If you would like to change the key of an application, delete the existing application and then create a new one")
      )
    }

  }

}
