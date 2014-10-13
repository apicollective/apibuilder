package models

import codegenerator.models.{Resource, ServiceDescription}
import com.gilt.apidoc.models.{ Organization, Service, User, Target }
import play.api.Play.current

case class MainTemplate(
  title: String,
  org: Option[Organization] = None,
  service: Option[Service] = None,
  version: Option[String] = None,
  serviceDescription: Option[ServiceDescription] = None,
  allServiceVersions: Seq[String] = Seq.empty,
  user: Option[User] = None,
  resource: Option[Resource] = None,
  settings: Option[SettingsMenu] = None,
  targets: Seq[Target] = Seq.empty,
  isOrgAdmin: Boolean = false,
  isOrgMember: Boolean = false
)

object MainTemplate {

  val gitVersion = current.configuration.getString("git.version").getOrElse {
    sys.error("git.version is required")
  }

  val supportEmail = current.configuration.getString("apidoc.support_email").getOrElse {
    sys.error("apidoc.support_email is required")
  }


}

case class SettingsMenu(
  section: Option[SettingSection] = None
)

case class SettingSection(name: String)
object SettingSection {
  val Domains = SettingSection("domains")
  val Members = SettingSection("members")
  val Metadata = SettingSection("metadata")
}
