package models

import com.bryzek.apidoc.api.v0.models.{Attribute, Application, GeneratorService, GeneratorWithService, Organization, User, Version}
import com.bryzek.apidoc.spec.v0.models.{Resource, Service}
import play.api.Play.current

case class UserTimeZone(
  name: String,
  label: String
)

object UserTimeZone {

  val Default = UserTimeZone(name = "US/Eastern", label = "EST")

  def apply(user: User): UserTimeZone = {
    // TODO
    Default
  }

}

case class MainTemplate(
  title: Option[String] = None,
  headTitle: Option[String] = None,
  org: Option[Organization] = None,
  application: Option[Application] = None,
  version: Option[String] = None,
  versionObject: Option[Version] = None,
  allServiceVersions: Seq[String] = Seq.empty,
  user: Option[User] = None,
  resource: Option[Resource] = None,
  settings: Option[SettingsMenu] = None,
  generators: Seq[GeneratorWithService] = Seq.empty,
  isOrgAdmin: Boolean = false,
  isOrgMember: Boolean = false,
  service: Option[Service] = None,
  query: Option[String] = None,
  requestPath: String
) {

  // Placeholder so that we can eventually choose timezone by user
  def timeZone: UserTimeZone = UserTimeZone.Default

  def canEditAttribute(attribute: Attribute): Boolean = {
    Some(attribute.audit.createdBy.guid) == user.map(_.guid)

  }

  def canEditApplication(applicationKey: String): Boolean = isOrgMember

  def canAdminApplication(applicationKey: String): Boolean = isOrgMember

  def canDeleteOrganization(): Boolean = isOrgAdmin

  /**
    * We allow only the author of a generator to delete it
    */
  def canDeleteGeneratorService(service: GeneratorService): Boolean = {
    user match {
      case None => false
      case Some(u) => u.guid == service.audit.createdBy.guid
    }
  }

}

object MainTemplate {

  val gitVersion = current.configuration.getString("git.version").getOrElse {
    sys.error("git.version is required")
  }

  val supportEmail = current.configuration.getString("apidoc.supportEmail").getOrElse {
    sys.error("apidoc.supportEmail is required")
  }

}

// TODO: Remove this class and use Option[SettingSection] directly
case class SettingsMenu(
  section: Option[SettingSection] = None
)

case class SettingSection(name: String)
object SettingSection {
  val Attributes = SettingSection("attributes")
  val Details = SettingSection("details")
  val Domains = SettingSection("domains")
  val Members = SettingSection("members")
  val Generators = SettingSection("generators")
}
