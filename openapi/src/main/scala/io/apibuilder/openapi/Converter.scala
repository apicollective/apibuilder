package io.apibuilder.openapi

import io.apibuilder.spec.v0.models._
import lib.{ServiceConfiguration, UrlKey}

object Converter {

  def convert(
    openApi: sttp.apispec.openapi.OpenAPI,
    config: ServiceConfiguration,
    filterHeaders: Set[String] = Set.empty,
    nameOverride: Option[String] = None,
  ): Service = {
    val namingConfig = NamingConfig()
    val c = Classification.fromOpenApi(openApi, namingConfig, filterHeaders)
    val schemaConverter = new SchemaConverter(c.modelReferences, namingConfig)
    val schemaResult = schemaConverter.convert(c.classification)

    val apiName = nameOverride.getOrElse(UrlKey.generate(openApi.info.title))

    Service(
      apidoc = None,
      name = openApi.info.title,
      organization = Organization(key = config.orgKey),
      application = Application(key = apiName),
      namespace = config.applicationNamespace(apiName),
      version = config.version,
      baseUrl = openApi.servers.headOption.map(_.url),
      description = openApi.info.description,
      info = Info(license = None, contact = None),
      headers = c.securityHeaders,
      imports = Seq.empty,
      enums = schemaResult.enums,
      interfaces = Seq.empty,
      unions = schemaResult.unions,
      models = schemaResult.models,
      resources = c.pathResult.resources,
      attributes = Seq.empty,
      annotations = Seq.empty,
    )
  }
}
