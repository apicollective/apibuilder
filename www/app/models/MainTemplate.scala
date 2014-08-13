package models

import core.{ Enum, Model, Resource, ServiceDescription }
import apidoc.models.{ Organization, Service, User }
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
  settings: Option[SettingsMenu] = None
)

object MainTemplate {

  val apidocVersion = current.configuration.getString("apidoc.version").getOrElse {
    sys.error("apidoc.version is required")
  }

}

case class SettingsMenu(
  section: Option[SettingSection] = None
)

case class SettingSection(name: String)
object SettingSection {
  val Domains = SettingSection("domains")
  val Members = SettingSection("members")
}

