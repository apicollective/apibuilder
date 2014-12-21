package models

import com.gilt.apidocgenerator.models.{Resource, ServiceDescription}
import com.gilt.apidoc.models.{ Organization, Service, User, Generator }
import com.gilt.apidocspec
import play.api.Play.current

case class MainTemplate(
  title: Option[String] = None,
  headTitle: Option[String] = None,
  org: Option[Organization] = None,
  service: Option[Service] = None,
  version: Option[String] = None,
  serviceDescription: Option[ServiceDescription] = None,
  allServiceVersions: Seq[String] = Seq.empty,
  user: Option[User] = None,
  resource: Option[Resource] = None,
  settings: Option[SettingsMenu] = None,
  generators: Seq[Generator] = Seq.empty,
  isOrgAdmin: Boolean = false,
  isOrgMember: Boolean = false,
  specService: Option[apidocspec.models.Service] = None,
  requestPath: String
) {

  def canEditService(service: String): Boolean = isOrgMember

  def canDeleteService(service: String): Boolean = isOrgMember

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
  val Domains = SettingSection("domains")
  val Members = SettingSection("members")
  val Metadata = SettingSection("metadata")
  val Generators = SettingSection("generators")
}
