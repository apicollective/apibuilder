package lib

case class ExampleService(key: String) {

  val docsUrl = s"/gilt/$key/latest"
  val apiJsonUrl = s"/gilt/$key/latest/api.json"

}

object Util {

  val AddApplicationText = "Add Application"
  val OrgSettingsText = "Org Settings"

  val ApidocExample = ExampleService("apidoc")
  val ApidocGeneratorExample = ExampleService("apidoc-generator")
  val ApidocSpecExample = ExampleService("apidoc-spec")
  val Examples = Seq(ApidocExample, ApidocGeneratorExample, ApidocSpecExample)

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

}
