package lib

case class ExampleService(key: String, version: String = "latest") {

  val docsUrl = if (version == "latest") {
    s"/gilt/$key"
  } else {
    s"/gilt/$key/$version"
  }

  val originalJsonUrl = s"/gilt/$key/$version/original.json"
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
  val GeneratorGitHubUrl = "https://github.com/gilt/apidoc-generator"

  def fullUrl(stub: String): String = s"$Host$stub"

}
