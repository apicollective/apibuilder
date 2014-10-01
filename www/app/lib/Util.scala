package lib

import core.VersionTag

object Util {

  val AddServiceText = "Add Service"
  val ApiDocExampleUrl = "/gilt/docs/api-doc/latest"
  val ApiDocExampleApiJsonUrl = "/gilt/api.json/api-doc/latest"

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
