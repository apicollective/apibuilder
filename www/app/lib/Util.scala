package lib

import io.apibuilder.apidoc.api.v0.models.{ApplicationSummary, ItemDetail, ItemDetailUndefinedType}

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

  private[this] val ApiHost = Config.requiredString("apibuilder.api.host")
  val Host = Config.requiredString("apibuilder.www.host")

  val SubscriptionsText = "Subscriptions"
  val SubscriptionsVersionsCreateText = "For applications that I watch, email me when a version is created."

  val AddApplicationText = "Add Application"
  val OrgDetailsText = "Org Details"
  val OrgAttributesText = "Org Attributes"
  val ServiceJsonText = "service.json"

  val ApidocApi = ExampleService("bryzek", "apidoc-api")
  val ApidocExample = ApidocApi
  val ApidocExampleWithVersionNumber = ExampleService("bryzek", "apidoc-api", Config.requiredString("git.version"))
  val ApidocGeneratorExample = ExampleService("bryzek", "apidoc-generator")
  val ApidocSpecExample = ExampleService("bryzek", "apidoc-spec")
  val Examples = Seq(ApidocExample, ApidocGeneratorExample, ApidocSpecExample)

  private val gitHub = "https://github.com/mbryzek"
  val ApidocGitHubUrl = s"$gitHub/apidoc"
  val ApidocCliGitHubUrl = s"$gitHub/apidoc-cli"
  val ApidocGeneratorGitHubUrl = s"$gitHub/apidoc-generator"

  def fullUrl(stub: String): String = s"$Host$stub"
  def fullApiUrl(stub: String): String = s"$ApiHost$stub"

  def formatUri(value: String): String = {
    if (value.toLowerCase.trim.startsWith("http")) {
      value
    } else {
      "http://" + value
    }
  }

  private[this] val WhiteListedDomains = Seq(
    "http://apidoc.me", "http://www.apidoc.me",
    "https://apidoc.me", "https://www.apidoc.me"
  )

  def validateReturnUrl(value: String): Either[Seq[String], String] = {
    if (value.startsWith("/")) {
      Right(value)
    } else {
      WhiteListedDomains.find { d => value.startsWith(d) } match {
        case None => Left(Seq(s"Redirect URL[$value] must start with / or a known domain"))
        case Some(d) => Right(
          value.substring(d.length).trim match {
            case "" => "/"
            case trimmed => {
              assert(trimmed.startsWith("/"), s"Redirect URL[$value] must start with /")
              trimmed
            }
          }
        )
      }
    }
  }

  def searchUrl(detail: ItemDetail): Option[String] = {
    detail match {
      case io.apibuilder.apidoc.api.v0.models.ApplicationSummary(guid, org, key) => {
	Some(s"/${org.key}/${key}/latest")
      }
      case io.apibuilder.apidoc.api.v0.models.ItemDetailUndefinedType(desc) => {
	None
      }
    }
  }


}
