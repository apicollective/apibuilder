package lib

import core.VersionTag
import com.gilt.apidocgenerator.models.Container

object Util {

  val AddServiceText = "Add Service"
  val OrgSettingsText = "Org Settings"
  val ApiDocExampleUrl = "/gilt/docs/apidoc/latest"
  val ApiDocExampleApiJsonUrl = "/gilt/api.json/apidoc/latest"
  val GitHubUrl = "https://github.com/gilt/apidoc"

  def calculateNextVersion(version: String): String = {
    version.split(VersionTag.Dash).size match {
      case 1 => {
        val pieces = version.split(VersionTag.Dot)
        if (pieces.forall(s => VersionTag.isDigit(s))) {
          (Seq(pieces.last.toInt + 1) ++ pieces.reverse.drop(1)).reverse.mkString(".")
        } else {
          version
        }
      }
      case _ => version
    }
  }

  def formatType(container: Container, name: String) = {
    container match {
      case Container.Singleton => name
      case Container.List => s"[$name]"
      case Container.Map => s"map[$name]"
      case Container.UNDEFINED(container) => s"$container[$name]"
    }
  }

}
