package lib

import com.gilt.apidocgenerator.models.Container

case class ExampleService(key: String) {

  val docsUrl = s"/gilt/$key/latest"
  val apiJsonUrl = s"/gilt/$key/latest/api.json"

}

object Util {

  val AddServiceText = "Add Service"
  val OrgSettingsText = "Org Settings"

  val ApidocExample = ExampleService("apidoc")
  val ApidocGeneratorExample = ExampleService("apidoc-generator")
  val Examples = Seq(ApidocExample, ApidocGeneratorExample)

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
