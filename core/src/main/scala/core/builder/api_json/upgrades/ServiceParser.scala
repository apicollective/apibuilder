package builder.api_json.upgrades

import cats.data.ValidatedNec
import cats.implicits._
import io.apibuilder.spec.v0.models.json._
import io.apibuilder.spec.v0.models.{Apidoc, Service}
import play.api.Logger
import play.api.libs.json._

import scala.annotation.nowarn
import scala.util.{Failure, Success, Try}

/**
 * This class is used to add data to the service specification that we have deprecated
 * but which may be required for older versions of clients.
 *
 * For example, we deprecated the 'apidoc' node which is now Optional in the Service specification.
 * If, however, we serve a Service JSON object without the API DOC node, a client with an older
 * service specification may fail to parse the json as the field will be expected to be required.
 */
case class ServiceParser() {

  private val logger: Logger = Logger(this.getClass)

  def fromString(js: String): ValidatedNec[String, Service] = {
    Try {
      Json.parse(js)
    } match {
      case Success(j) => fromJson(j)
      case Failure(ex) => s"Invalid JSON: ${ex.getMessage}".invalidNec
    }
  }

  def fromJson(js: JsValue): ValidatedNec[String, Service] = {
    js match {
      case o: JsObject => {
        o.validate[Service] match {
          case JsSuccess(s, _) => upgrade(s).validNec
          case JsError(errors) => errors.map { case (path, msg) =>
            s"${path.path.map(_.toJsonString).mkString("/")}: ${msg.flatMap(_.messages).mkString(", ")}"
          }.mkString(", ").invalidNec
        }
      }
      case _ => "Can only parse service from a JSON Object".invalidNec
    }
  }

  private val DefaultApidoc: Apidoc = Apidoc(version = "1.0.0")

  @nowarn("msg=value apidoc in class Service is deprecated: This field is no longer used in API Builder and may be removed in the future.")
  private def upgrade(service: Service): Service = {
    service.apidoc match {
      case Some(_) => service
      case None => {
        logger.info("Upgrading service JSON to add default apidoc version")
        service.copy(apidoc = Some(DefaultApidoc))
      }
    }
  }

}
