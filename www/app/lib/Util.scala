package lib

import com.bryzek.apidoc.api.v0.models.{ApplicationSummary, ItemDetail, ItemDetailUndefinedType}

case class ExampleService(
  organizationKey: String,
  applicationKey: String,
  version: String = "latest"
) {

  val label = s"$organizationKey/$applicationKey"

  val docsUrl = if (version == "latest") {
    s"/$organizationKey/$applicationKey"
  } else {
    s"/$organizationKey/$applicationKey/$version"
  }

  val originalJsonUrl = s"/$organizationKey/$applicationKey/$version/original"
  val serviceJsonUrl = s"/$organizationKey/$applicationKey/$version/service.json"

}

object Util {

  val Host = Config.requiredString("apidoc.www.host")

  val SubscriptionsText = "Subscriptions"
  val SubscriptionsVersionsCreateText = "For applications that I watch, email me when a version is created."

  val AddApplicationText = "Add Application"
  val OrgDetailsText = "Org Details"
  val ServiceJsonText = "service.json"

  val ApidocExample = ExampleService("mbryzek", "apidoc-api")
  val ApidocExampleWithVersionNumber = ExampleService("mbryzek", "apidoc-api", Config.requiredString("git.version"))
  val ApidocGeneratorExample = ExampleService("mbryzek", "apidoc-generator")
  val ApidocSpecExample = ExampleService("mbryzek", "apidoc-spec")
  val Examples = Seq(ApidocExample, ApidocGeneratorExample, ApidocSpecExample)

  private val gitHub = "https://github.com/mbryzek"
  val ApidocGitHubUrl = s"$gitHub/apidoc"
  val ApidocCliGitHubUrl = s"$gitHub/apidoc-cli"
  val GeneratorGitHubUrl = s"$gitHub/apidoc-generator"

  def fullUrl(stub: String): String = s"$Host$stub"

  def formatUri(value: String): String = {
    if (value.toLowerCase.trim.startsWith("http")) {
      value
    } else {
      "http://" + value
    }
  }

  def searchUrl(detail: ItemDetail): Option[String] = {
    detail match {
      case com.bryzek.apidoc.api.v0.models.ApplicationSummary(guid, org, key) => {
	Some(s"/${org.key}/${key}/latest")
      }
      case com.bryzek.apidoc.api.v0.models.ItemDetailUndefinedType(desc) => {
	None
      }
    }
  }


}
