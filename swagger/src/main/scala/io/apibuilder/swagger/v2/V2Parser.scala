package io.apibuilder.swagger.v2

import cats.data.ValidatedNec
import cats.implicits._
import io.apibuilder.spec.v0.models._
import io.swagger.v3.oas.models.{OpenAPI, info}
import io.swagger.v3.parser.OpenAPIV3Parser
import lib.{ServiceConfiguration, UrlKey}
import scala.jdk.CollectionConverters._

case class V2Parser(config: ServiceConfiguration) {

  // Set to the version of API Builder at which we built the parser
  private[this] val ApiDocConstant = Apidoc(version = "0.15.58")

  def parse(contents: String): ValidatedNec[String, Service] = {
    parseContents(contents).andThen { openApi =>
      parseOpenApi(openApi)
    }
  }

  private[this] def parseContents(contents: String): ValidatedNec[String, OpenAPI] = {
    val result = new OpenAPIV3Parser().readContents(contents, null, null)
    val errors = Option(result.getMessages).map(_.asScala).getOrElse(Nil)
    if (errors.nonEmpty) {
      errors.mkString(", ").invalidNec
    } else {
      Option(result.getOpenAPI) match {
        case None => "Unknown error parsing contents".invalidNec
        case Some(openApi) => openApi.validNec
      }
    }
  }

  private[this] def parseOpenApi(api: OpenAPI): ValidatedNec[String, Service] = {
    (
      validateName(api),
      validateInfo(api)
    ).mapN { case (name, info) =>
      Service(
        apidoc = ApiDocConstant,
        name = name,
        organization = Organization(key = config.orgKey),
        application = applicationFromName(name),
        namespace = config.orgNamespace,
        version = config.version,
        info = info,
      )
    }
  }

  private[this] def validateName(api: OpenAPI): ValidatedNec[String, String] = {
    Option(api.getInfo).flatMap { info => trimmedString(info.getTitle) } match {
      case Some(title) => title.validNec
      case _ => "info/title must not  be blank".invalidNec
    }
  }

  private[this] def applicationFromName(name: String): Application = {
    Application(key = UrlKey.generate(name))
  }

  private[this] def validateInfo(api: OpenAPI): ValidatedNec[String, Info] = {
    (validateLicense(api), validateContact(api)).mapN { case (license, contact) =>
      Info(
        license = license,
        contact = contact,
      )
    }
  }

  private[this] def validateLicense(api: OpenAPI): ValidatedNec[String, Option[License]] = {
    Option(api.getInfo).flatMap { info => Option(info.getLicense) } match {
      case None => None.validNec
      case Some(l) => {
        (validateLicenseName(l), validateLicenseUrl(l)).mapN { case (name, url) =>
          Some(License(name = name, url = url))
        }
      }
    }
  }

  private[this] def validateLicenseName(l: info.License): ValidatedNec[String, String] = {
    trimmedString(l.getName) match {
      case None => "info/license/name is required when specifying license information".invalidNec
      case Some(name) => name.validNec
    }
  }

  private[this] def validateLicenseUrl(l: info.License): ValidatedNec[String, Option[String]] = {
    trimmedString(l.getUrl).validNec
  }

  private[this] def validateContact(api: OpenAPI): ValidatedNec[String, Option[Contact]] = {
    Option(api.getInfo).flatMap { info => Option(info.getContact) }.map { c =>
      Contact(
        name = trimmedString(c.getName),
        url = trimmedString(c.getUrl),
        email = trimmedString(c.getEmail)
      )
    }.validNec
  }

  private[this] def trimmedString(value: String): Option[String] = Option(value).map(_.trim).filter(_.nonEmpty)
}