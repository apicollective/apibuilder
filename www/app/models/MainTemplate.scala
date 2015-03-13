package models

import com.gilt.apidoc.v0.models.{Application, Generator, Organization, User, Version}
import com.gilt.apidoc.spec.v0.models.{Resource, Service}
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
  generators: Seq[Generator] = Seq.empty,
  isOrgAdmin: Boolean = false,
  isOrgMember: Boolean = false,
  service: Option[Service] = None,
  requestPath: String
) {

  // Placeholder so that we can eventually choose timezone by user
  def timeZone: UserTimeZone = UserTimeZone.Default

  def canEditApplication(applicationKey: String): Boolean = isOrgMember

  def canDeleteApplication(applicationKey: String): Boolean = isOrgMember

  def canDeleteOrganization(): Boolean = isOrgAdmin

}

object MainTemplate {

  val gitVersion = current.configuration.getString("git.version").getOrElse {
    sys.error("git.version is required")
  }

  val supportEmail = current.configuration.getString("apidoc.supportEmail").getOrElse {
    sys.error("apidoc.supportEmail is required")
  }

}

case class SettingsMenu(
  section: Option[SettingSection] = None
)

case class SettingSection(name: String)
object SettingSection {
  val Details = SettingSection("details")
  val Domains = SettingSection("domains")
  val Members = SettingSection("members")
  val Generators = SettingSection("generators")
}
