package db

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import java.util.UUID

class VersionValidatorSpec extends PlaySpec with OneAppPerSuite with util.Daos {

  "validates user is a member of the organization to create an application" in {
    val org = Util.createOrganization()

    VersionValidator(Util.createRandomUser(), org, UUID.randomUUID.toString).validate must be(
      Seq("You must be a member of this organization to update applications")
    )
  }

  "key" must {

    "validates if already exists" in {
      val org = Util.createOrganization()
      val existing = Util.createApplication(org)

      VersionValidator(Util.createdBy, org, existing.key).validate must be(
        Seq(s"An application with key[${existing.key}] already exists")
      )
    }

    "no errors for a new key" in {
      val org = Util.createOrganization()
      val existing = Util.createApplication(org)

      VersionValidator(Util.createdBy, org, UUID.randomUUID.toString).validate must be(Nil)
    }

    "no errors when updating an application with the same key" in {
      val org = Util.createOrganization()
      val existing = Util.createApplication(org)

      VersionValidator(Util.createdBy, org, existing.key, Some(existing.key)).validate must be(Nil)
    }

    "validates that key is not changing" in {
      val org = Util.createOrganization()
      val existing = Util.createApplication(org)

      val newKey = UUID.randomUUID.toString
      VersionValidator(Util.createdBy, org, newKey, Some(existing.key)).validate must be(
        Seq(s"The application key[$newKey] in the uploaded file does not match the existing application key[${existing.key}]. If you would like to change the key of an application, delete the existing application and then create a new one")
      )
    }

  }

}
