package io.apibuilder.swagger.v2

import cats.data.ValidatedNec
import cats.implicits._
import io.apibuilder.spec.v0.models._
import io.swagger.v3.oas.models.{OpenAPI, info}
import io.swagger.v3.parser.OpenAPIV3Parser
import lib.{ServiceConfiguration, UrlKey}
import scala.jdk.CollectionConverters._

object V2ParserConstants {
  // Set to the version of API Builder at which we built the parser
  val ApiDocConstant: Apidoc = Apidoc(version = "0.15.58")
}

case class V2Parser(config: ServiceConfiguration) {
  import V2ParserConstants._

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
      validateVersion(api),
      validateInfo(api),
      validateBaseUrl(api),
      validateDescription(api),
      Components.validate(api)
    ).mapN { case (name, version, info, baseUrl, description, components) =>
      Service(
        apidoc = ApiDocConstant,
        name = name,
        organization = Organization(key = config.orgKey),
        application = applicationFromName(name),
        namespace = config.orgNamespace,
        version = version,
        baseUrl = baseUrl,
        info = info,
        description = description,
        headers = Nil, // Not currently supported
        imports = Nil, // Not currently supported
        attributes = Nil, // Not currently supported
        annotations = Nil, // Not currently supported
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

  private[this] def validateVersion(api: OpenAPI): ValidatedNec[String, String] = {
    Option(api.getInfo).flatMap { info => Option(info.getVersion) } match {
      case None => "info/version is required".invalidNec
      case Some(v) => v.validNec
    }
  }

  private[this] def validateBaseUrl(api: OpenAPI): ValidatedNec[String, Option[String]] = {
    Option(api.getServers).map(_.asScala).getOrElse(Nil).flatMap(s => Option(s.getUrl)).toList.distinct match {
      case Nil => None.validNec
      case one :: Nil => Some(one).validNec
      case _ => "API Builder requires at most 1 distinct server url".invalidNec
    }
  }

  private[this] def validateDescription(api: OpenAPI): ValidatedNec[String, Option[String]] = {
    Option(api.getInfo).flatMap { info => trimmedString(info.getDescription) }.validNec
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