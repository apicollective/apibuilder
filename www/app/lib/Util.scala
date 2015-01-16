package lib

case class ExampleService(key: String, version: String = "latest") {

  val docsUrl = if (version == "latest") {
    s"/gilt/$key"
  } else {
    s"/gilt/$key/$version"
  }

  val apiJsonUrl = s"/gilt/$key/$version/api.json"
  val serviceJsonUrl = s"/gilt/$key/$version/service.json"

}

object Util {

  val Host = Config.requiredString("apidoc.www.host")

  val AddApplicationText = "Add Application"
  val OrgDetailsText = "Org Details"
  val ServiceJsonText = "service.json"

  val ApidocExample = ExampleService("apidoc")
  val ApidocExampleWithVersionNumber = ExampleService("apidoc", Config.requiredString("git.version"))
  val ApidocGeneratorExample = ExampleService("apidoc-generator")
  val ApidocSpecExample = ExampleService("apidoc-spec")
  val Examples = Seq(ApidocExample, ApidocGeneratorExample, ApidocSpecExample)

  val GitHubUrl = "https://github.com/gilt/apidoc"

  def fullUrl(stub: String): String = s"$Host$stub"

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
