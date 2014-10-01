package lib

import core.VersionTag

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

  def formatType(typeName: String, isMultiple: Boolean) = {
    if (isMultiple) {
      s"[$typeName]"
    } else {
      typeName
    }
  }

}
