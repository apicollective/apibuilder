package builder

import me.apidoc.swagger.SwaggerServiceValidator
import me.apidoc.avro.AvroIdlServiceValidator
import api_json.ApiJsonServiceValidator
import lib.{ServiceConfiguration, ServiceValidator}
import core.{ClientFetcher, ServiceFetcher}
import com.gilt.apidoc.api.v0.models.{Original, OriginalType}
import com.gilt.apidoc.spec.v0.models.Service

object OriginalValidator {

  // TODO: if valid, need to use ServiceSpecValidator.scala

  def apply(
    config: ServiceConfiguration,
    original: Original,
    fetcher: ServiceFetcher = new ClientFetcher(),
    internalMigration: Boolean = false
  ): ServiceValidator[Service] = {
    val validator = original.`type` match {
      case OriginalType.ApiJson => {
        ApiJsonServiceValidator(config, original.data, fetcher, internalMigration)
      }
      case OriginalType.AvroIdl => {
        AvroIdlServiceValidator(config, original.data)
      }
      case OriginalType.SwaggerJson => {
        SwaggerServiceValidator(config, original.data)
      }
      case _ => {
        sys.error("Invalid original type: " + original.`type`)
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

