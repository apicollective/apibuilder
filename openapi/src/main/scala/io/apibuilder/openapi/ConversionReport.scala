package io.apibuilder.openapi

import sttp.apispec.openapi.OpenAPI

case class SchemaReport(name: String, kind: SchemaKind)
case class FieldReport(schemaName: String, fieldName: String, kind: Option[FieldKind], ignoredFormat: Option[String] = None)
case class PathReport(path: String, methods: Seq[String], unsupported: Seq[String])

case class ConversionReport(
  schemas: Seq[SchemaReport],
  fields: Seq[FieldReport],
  paths: Seq[PathReport],
  unsupportedFeatures: Seq[String],
) {

  def models: Seq[String] = schemas.collect { case SchemaReport(n, SchemaKind.Object) => n }
  def enums: Seq[String] = schemas.collect { case SchemaReport(n, SchemaKind.StringEnum) => n }
  def arrays: Seq[String] = schemas.collect { case SchemaReport(n, SchemaKind.Array) => n }
  def unions: Seq[String] = schemas.collect { case SchemaReport(n, SchemaKind.Union) => n }
  def aliases: Seq[String] = schemas.collect { case SchemaReport(n, SchemaKind.Alias) => n }
  def skipped: Seq[String] = schemas.collect { case SchemaReport(n, SchemaKind.Skip) => n }

  def unmappedFields: Seq[String] = fields.collect { case FieldReport(schema, field, None, _) => s"$schema.$field" }

  def defaultedFields: Seq[String] = fields.collect {
    case FieldReport(schema, field, Some(FieldKind.DefaultedString), _) => s"$schema.$field"
  }

  def ignoredFormats: Seq[String] = fields.collect { case FieldReport(schema, field, _, Some(fmt)) =>
    s"$schema.$field (format: $fmt)"
  }

  def briefSummary: String = {
    val pathIssueCount = paths.flatMap(_.unsupported).size
    val parts = Seq(
      Option.when(unmappedFields.nonEmpty)(s"${unmappedFields.size} unmapped fields"),
      Option.when(defaultedFields.nonEmpty)(s"${defaultedFields.size} fields defaulted to string"),
      Option.when(ignoredFormats.nonEmpty)(s"${ignoredFormats.size} ignored formats"),
      Option.when(pathIssueCount > 0)(s"$pathIssueCount path issues"),
      Option.when(unsupportedFeatures.nonEmpty)(s"${unsupportedFeatures.size} unsupported features"),
    ).flatten
    if (parts.isEmpty) "Imported from OpenAPI."
    else s"Imported from OpenAPI. Conversion issues: ${parts.mkString(", ")}."
  }

  def summary: String = {
    val lines = Seq.newBuilder[String]
    lines += s"=== Conversion Report ==="
    lines += s"Schemas: ${schemas.size} total"
    lines += s"  Models:  ${models.size}"
    lines += s"  Enums:   ${enums.size}"
    lines += s"  Arrays:  ${arrays.size}"
    lines += s"  Unions:  ${unions.size}"
    lines += s"  Aliases: ${aliases.size} (resolved, not emitted)"
    lines += s"  Skipped: ${skipped.size}"
    if (unmappedFields.nonEmpty) {
      lines += s"Unmapped fields: ${unmappedFields.size}"
      unmappedFields.foreach(f => lines += s"  - $f")
    }
    if (defaultedFields.nonEmpty) {
      lines += s"Defaulted to string (no type in spec): ${defaultedFields.size}"
      defaultedFields.foreach(f => lines += s"  - $f")
    }
    if (ignoredFormats.nonEmpty) {
      lines += s"Ignored formats: ${ignoredFormats.size}"
      ignoredFormats.foreach(f => lines += s"  - $f")
    }
    lines += s"Paths: ${paths.size}"
    val pathIssues = paths.flatMap(_.unsupported)
    if (pathIssues.nonEmpty) {
      lines += s"Path issues: ${pathIssues.size}"
      pathIssues.foreach(i => lines += s"  - $i")
    }
    if (unsupportedFeatures.nonEmpty) {
      lines += s"Unsupported features: ${unsupportedFeatures.size}"
      unsupportedFeatures.foreach(f => lines += s"  - $f")
    }
    lines.result().mkString("\n")
  }
}

object ConversionReport {

  def fromClassification(c: Classification): ConversionReport = {
    val schemaReports = c.classification.schemas.map(cs => SchemaReport(cs.name, cs.kind))
    val fieldReports = c.classification.schemas.flatMap(_.fields.map { cf =>
      FieldReport(cf.schemaName, cf.fieldName, cf.kind, cf.annotations.ignoredFormat)
    })
    ConversionReport(schemaReports, fieldReports, c.pathResult.pathReports, c.unsupportedFeatures)
  }

  def detectUnsupportedFeatures(openApi: OpenAPI): Seq[String] = {
    val features = Seq.newBuilder[String]

    openApi.components.foreach { c =>
      c.securitySchemes.foreach {
        case (name, Right(scheme)) if !SecurityConverter.isConvertible(scheme) =>
          features += s"securityScheme '$name': type '${scheme.`type`}' (not converted)"
        case (name, Left(_)) =>
          features += s"securityScheme '$name': reference (not resolved)"
        case _ => ()
      }
      if (c.requestBodies.nonEmpty)
        features += s"requestBodies: ${c.requestBodies.size} defined (not converted as standalone)"
      if (c.headers.nonEmpty)
        features += s"headers: ${c.headers.size} defined (not converted)"
      if (c.links.nonEmpty)
        features += s"links: ${c.links.size} defined (not converted)"
      if (c.callbacks.nonEmpty)
        features += s"callbacks: ${c.callbacks.size} defined (not converted)"
    }

    if (openApi.security.nonEmpty)
      features += s"global security: ${openApi.security.size} requirements (not converted)"
    if (openApi.tags.nonEmpty)
      features += s"tags: ${openApi.tags.size} defined (not converted)"
    if (openApi.extensions.nonEmpty)
      features += s"extensions: ${openApi.extensions.size} vendor extensions (not converted)"

    features.result()
  }
}
