package io.apibuilder.openapi

import io.apibuilder.spec.v0.models._
import lib.{ServiceConfiguration, UrlKey}
import play.api.libs.json.Json

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
    val report = ConversionReport.fromClassification(c)
    val description = Seq(openApi.info.description, Some(report.briefSummary)).flatten.mkString("\n\n")
    val conversionAttribute = buildConversionAttribute(report)

    Service(
      apidoc = None,
      name = openApi.info.title,
      organization = Organization(key = config.orgKey),
      application = Application(key = apiName),
      namespace = config.applicationNamespace(apiName),
      version = config.version,
      baseUrl = openApi.servers.headOption.map(_.url),
      description = Some(description),
      info = Info(license = None, contact = None),
      headers = c.securityHeaders,
      imports = Seq.empty,
      enums = schemaResult.enums,
      interfaces = Seq.empty,
      unions = schemaResult.unions,
      models = schemaResult.models,
      resources = c.pathResult.resources,
      attributes = Seq(conversionAttribute),
      annotations = Seq.empty,
    )
  }

  private def buildConversionAttribute(report: ConversionReport): Attribute = {
    val pathIssues = report.paths.flatMap(_.unsupported)
    val value = Json.obj(
      "unmapped_fields"      -> report.unmappedFields,
      "defaulted_fields"     -> report.defaultedFields,
      "ignored_formats"      -> report.ignoredFormats,
      "path_issues"          -> pathIssues,
      "unsupported_features" -> report.unsupportedFeatures,
    )
    Attribute(name = "openapi_conversion", value = value)
  }
}
