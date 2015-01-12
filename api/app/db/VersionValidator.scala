package db

import com.gilt.apidoc.models.{Application, Organization, User}
import lib.Validation
import core.ServiceValidator

case class VersionValidator(
  user: User,
  org: Organization,
  newApplicationKey: String,
  existingApplicationKey: Option[String] = None
) {

  val validate: Seq[String] = validateAuthorization() ++ validateKey()

  private lazy val existing = existingApplicationKey.flatMap { ApplicationsDao.findByOrganizationKeyAndApplicationKey(Authorization.All, org.key, _) }

  private def validateAuthorization(): Seq[String] = {
    MembershipsDao.isUserMember(user, org) match {
      case true => Seq.empty
      case false => Seq("You must be a member of this organization to update applications")
    }
  }

  private def validateKey(): Seq[String] = {
    existing match {
      case None => {
        ApplicationsDao.findByOrganizationKeyAndApplicationKey(Authorization.All, org.key, newApplicationKey) match {
          case None => Seq.empty
          case Some(app) => Seq(s"An application with key[$newApplicationKey] already exists")
        }
      }

      case Some(app) => {
        if (app.key == newApplicationKey) {
          Seq.empty
        } else {
          Seq(s"The application key[$newApplicationKey] in the uploaded file does not match the existing application key[${app.key}]. If you would like to change the key of an application, delete the existing application and then create a new one")
        }
      }
    }
  }

}
