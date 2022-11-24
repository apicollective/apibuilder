package builder

import builder.api_json.{ApiJsonServiceValidator, ServiceJsonServiceValidator}
import cats.data.ValidatedNec
import core.{ServiceFetcher, VersionMigration}
import io.apibuilder.api.v0.models.OriginalType
import io.apibuilder.avro.AvroIdlServiceValidator
import io.apibuilder.spec.v0.models.Service
import io.apibuilder.swagger.SwaggerServiceValidator
import lib.{ServiceConfiguration, ServiceValidator}

object OriginalValidator {

  def apply(
    config: ServiceConfiguration,
    `type`: OriginalType,
    fetcher: ServiceFetcher,
    migration: VersionMigration = VersionMigration(internal = false)
  ): ServiceValidator[Service] = {
    val validator = `type` match {
      case OriginalType.ApiJson => {
        ApiJsonServiceValidator(config, fetcher, migration)
      }
      case OriginalType.AvroIdl => {
        AvroIdlServiceValidator(config)
      }
      case OriginalType.ServiceJson => {
        ServiceJsonServiceValidator
      }
      case OriginalType.Swagger => {
        SwaggerServiceValidator(config)
      }
      case OriginalType.UNDEFINED(other) => {
        sys.error(s"Invalid original type[$other]")
      }
    }
    WithServiceSpecValidator(validator)
  }

  case class WithServiceSpecValidator(underlying: ServiceValidator[Service]) extends ServiceValidator[Service] {

    override def validate(rawInput: String): ValidatedNec[String, Service] = {
      underlying.validate(rawInput).andThen { service =>
        ServiceSpecValidator(service).validate()
      }
    }

  }

}

