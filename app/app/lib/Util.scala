package lib

import javax.inject.Inject

import io.apibuilder.api.v0.models.ItemDetail

case class ExampleService(
  organizationKey: String,
  applicationKey: String,
  version: String = "latest"
) {

  val label: String = s"$organizationKey/$applicationKey"

  val docsUrl: String = if (version == "latest") {
    s"/$organizationKey/$applicationKey"
  } else {
    s"/$organizationKey/$applicationKey/$version"
  }

  val originalJsonUrl: String = s"/$organizationKey/$applicationKey/$version/original"
  val serviceJsonUrl: String = s"/$organizationKey/$applicationKey/$version/service.json"

}

object  Labels {
  val SubscriptionsText = "Subscriptions"
  val SubscriptionsVersionsCreateText = "For applications that I watch, email me when a version is created."
  val SubscriptionsVersionsMaterialChangeText = "For applications that I watch, email me when a version is created with a material change."

  val AddApplicationText = "Add Application"
  val OrgDetailsText = "Org Details"
  val OrgAttributesText = "Org Attributes"
  val ServiceJsonText = "service.json"

  val ApiBuilderApi: ExampleService = ExampleService("apicollective", "apibuilder-api")
  val ApiBuilderApiJson: ExampleService = ExampleService("apicollective", "apibuilder-api-json")
  val ApiBuilderExample: ExampleService = ApiBuilderApi
  val ApiBuilderExampleWithVersionNumber: ExampleService = ExampleService("apicollective", "apibuilder-api", "0.13.29")
  val ApiBuilderGeneratorExample: ExampleService = ExampleService("apicollective", "apibuilder-generator")
  val ApiBuilderSpecExample: ExampleService = ExampleService("apicollective", "apibuilder-spec")
  val Examples: List[ExampleService] = List(ApiBuilderExample, ApiBuilderGeneratorExample, ApiBuilderSpecExample)

  private val gitHub = "https://github.com/apicollective"
  val ApiBuilderGitHubUrl = s"$gitHub/apibuilder"
  val ApiBuilderCliGitHubUrl = s"$gitHub/apibuilder-cli"
  val ApiBuilderGeneratorGitHubUrl = s"$gitHub/apibuilder-generator"
  val ApiBuilderSwaggerGeneratorGitHubUrl = s"$gitHub/apibuilder-swagger-generator"
}

class Util @Inject() (
  config: Config
) {

  private val ApiHost: String = config.requiredString("apibuilder.api.host")
  val Host: String = config.requiredString("apibuilder.app.host")

  def fullUrl(stub: String): String = s"$Host$stub"
  def fullApiUrl(stub: String): String = s"$ApiHost$stub"

  def formatUri(value: String): String = {
    if (value.toLowerCase.trim.startsWith("http")) {
      value
    } else {
      "http://" + value
    }
  }

  private val ApprovedDomains = Seq(
    "http://apidoc.me", "http://www.apidoc.me",
    "https://apidoc.me", "https://www.apidoc.me",
    "http://apibuilder.io", "http://www.apibuilder.io", "http://app.apibuilder.io", "http://ui.apibuilder.io",
    "https://apibuilder.io", "https://www.apibuilder.io", "https://app.apibuilder.io", "https://ui.apibuilder.io"
  )

  def validateReturnUrl(value: String): Either[Seq[String], String] = {
    if (value.startsWith("/")) {
      Right(value)
    } else {
      ApprovedDomains.find(value.startsWith) match {
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
      case io.apibuilder.api.v0.models.ApplicationSummary(_, org, key) => Some(s"/${org.key}/$key/latest")
      case io.apibuilder.api.v0.models.ItemDetailUndefinedType(_) => None
    }
  }

}
