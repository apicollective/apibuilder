package builder

import builder.api_json.{ApiJsonServiceValidator, ServiceJsonServiceValidator}
import io.apibuilder.apidoc.api.v0.models.{Original, OriginalType}
import io.apibuilder.apidoc.spec.v0.models.Service
import core.{ServiceFetcher, VersionMigration}
import lib.{ServiceConfiguration, ServiceValidator}
import me.apidoc.avro.AvroIdlServiceValidator
import me.apidoc.swagger.SwaggerServiceValidator

object OriginalValidator {

  // TODO: if valid, need to use ServiceSpecValidator.scala

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

    override def validate(): Either[Seq[String], Service] = {
      underlying.validate() match {
        case Left(errors) => Left(errors)
        case Right(service) => {
          ServiceSpecValidator(service).errors match {
            case Nil => Right(service)
            case errors => Left(errors)
          }
        }
      }
    }

  }

}

