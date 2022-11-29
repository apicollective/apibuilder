package builder

import cats.data.ValidatedNec
import builder.api_json.{ApiJsonServiceValidator, ServiceJsonServiceValidator}
import io.apibuilder.api.v0.models.{Original, OriginalType}
import io.apibuilder.spec.v0.models.Service
import core.{ServiceFetcher, VersionMigration}
import lib.{ServiceConfiguration, ServiceValidator}
import io.apibuilder.avro.AvroIdlServiceValidator
import io.apibuilder.swagger.SwaggerServiceValidator

object OriginalValidator {

  def apply(
    config: ServiceConfiguration,
    original: Original,
    fetcher: ServiceFetcher,
    migration: VersionMigration = VersionMigration(internal = false)
  ): ServiceValidator[Service] = {
    val validator = original.`type` match {
      case OriginalType.ApiJson => {
        ApiJsonServiceValidator(config, original.data, fetcher, migration)
      }
      case OriginalType.AvroIdl => {
        AvroIdlServiceValidator(config, original.data)
      }
      case OriginalType.ServiceJson => {
        ServiceJsonServiceValidator(original.data)
      }
      case OriginalType.Swagger => {
        SwaggerServiceValidator(config, original.data)
      }
      case OriginalType.UNDEFINED(other) => {
        sys.error(s"Invalid original type[$other]")
      }
    }
    WithServiceSpecValidator(validator)
  }

  case class WithServiceSpecValidator(underlying: ServiceValidator[Service]) extends ServiceValidator[Service] {

    override def validate(): ValidatedNec[String, Service] = {
      underlying.validate().andThen { service =>
        ServiceSpecValidator(service).validate()
      }
    }

  }

}

