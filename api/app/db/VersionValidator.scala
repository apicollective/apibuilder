package db

import io.apibuilder.api.v0.models.{Organization, User}
import lib.Validation

case class VersionValidator(
  user: User,
  org: Organization,
  newApplicationKey: String,
  existingApplicationKey: Option[String] = None
) {

  // TODO: Inject directly
  private[this] def applicationsDao = play.api.Play.current.injector.instanceOf[ApplicationsDao]
  private[this] def membershipsDao = play.api.Play.current.injector.instanceOf[MembershipsDao]

  val validate: Seq[String] = validateAuthorization() ++ validateKey()

  private lazy val existing = existingApplicationKey.flatMap { applicationsDao.findByOrganizationKeyAndApplicationKey(Authorization.All, org.key, _) }

  private[this] def validateAuthorization(): Seq[String] = {
    membershipsDao.isUserMember(user, org) match {
      case true => Seq.empty
      case false => Seq("You must be a member of this organization to update applications")
    }
  }

  private[this] def validateKey(): Seq[String] = {
    existing match {
      case None => {
        applicationsDao.findByOrganizationKeyAndApplicationKey(Authorization.All, org.key, newApplicationKey) match {
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
