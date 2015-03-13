package builder

import core.{ClientFetcher, ServiceConfiguration, ServiceFetcher}
import com.gilt.apidoc.v0.models.{Original, OriginalType}
import com.gilt.apidoc.spec.v0.models.Service

trait ServiceValidator {

  def validate(): Either[Seq[String], Service]
  def errors(): Seq[String]
  def isValid: Boolean = errors.isEmpty

}

object ServiceValidator {

  def apply(
    config: ServiceConfiguration,
    original: Original,
    fetcher: ServiceFetcher = new ClientFetcher()
  ): ServiceValidator = {
    original.`type` match {
      case OriginalType.ApiJson => {
        api_json.ApiJsonServiceValidator(config, original.data, fetcher)
      }
      case _ => {
        sys.error("Invalid original type: " + original.`type`)
      }
    }
  }

}

