package db

import javax.inject.Inject
import io.apibuilder.api.v0.models.{Organization, User}

class VersionValidator @Inject() (
  applicationsDao: ApplicationsDao,
  membershipsDao: MembershipsDao
) {

  def validate(
    user: User,
    org: Organization,
    newApplicationKey: String,
    existingApplicationKey: Option[String] = None
  ): Seq[String] = {
    val authErrors= validateAuthorization(user, org)
    val keyErrors = validateKey(org, newApplicationKey, existingApplicationKey)
    authErrors ++ keyErrors
  }

  private def validateAuthorization(user: User, org: Organization): Seq[String] = {
    if (membershipsDao.isUserMember(user, org)) {
      Nil
    } else {
      Seq("You must be a member of this organization to update applications")
    }
  }

  private def validateKey(
    org: Organization,
    newApplicationKey: String,
    existingApplicationKey: Option[String]
  ): Seq[String] = {
    val existing = existingApplicationKey.flatMap { applicationsDao.findByOrganizationKeyAndApplicationKey(Authorization.All, org.key, _) }

    existing match {
      case None => {
        applicationsDao.findByOrganizationKeyAndApplicationKey(Authorization.All, org.key, newApplicationKey) match {
          case None => Nil
          case Some(_) => Seq(s"An application with key[$newApplicationKey] already exists")
        }
      }

      case Some(app) => {
        if (app.key == newApplicationKey) {
          Nil
        } else {
          Seq(s"The application key[$newApplicationKey] in the uploaded file does not match the existing application key[${app.key}]. If you would like to change the key of an application, delete the existing application and then create a new one")
        }
      }
    }
  }

}
