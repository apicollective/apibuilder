package io.apibuilder.openapi

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ConversionReportSpec extends AnyWordSpec with Matchers {

  private val emptyReport = ConversionReport(
    schemas = Seq.empty,
    fields = Seq.empty,
    paths = Seq.empty,
    unsupportedFeatures = Seq.empty,
  )

  private def fieldReport(schema: String, field: String, kind: Option[FieldKind]) =
    FieldReport(schema, field, kind)

  "briefSummary" must {

    "return clean message when there are no issues" in {
      emptyReport.briefSummary must be("Imported from OpenAPI.")
    }

    "include unmapped field count" in {
      val report = emptyReport.copy(fields = Seq(fieldReport("Foo", "bar", None)))
      report.briefSummary must include("1 unmapped fields")
    }

    "include defaulted-to-string field count" in {
      val report = emptyReport.copy(fields = Seq(fieldReport("Foo", "bar", Some(FieldKind.DefaultedString))))
      report.briefSummary must include("1 fields defaulted to string")
    }

    "include ignored format count" in {
      val report = emptyReport.copy(fields = Seq(
        FieldReport("Foo", "ts", Some(FieldKind.DefaultedString), ignoredFormat = Some("unixtime")),
      ))
      report.briefSummary must include("1 ignored formats")
    }

    "include path issue count" in {
      val report = emptyReport.copy(paths = Seq(
        PathReport("/foo", Seq("GET"), unsupported = Seq("GET /foo: some issue")),
      ))
      report.briefSummary must include("1 path issues")
    }

    "include unsupported feature count" in {
      val report = emptyReport.copy(unsupportedFeatures = Seq("webhooks: not converted"))
      report.briefSummary must include("1 unsupported features")
    }

    "combine multiple issue types in one sentence" in {
      val report = emptyReport.copy(
        fields = Seq(fieldReport("Foo", "bar", None)),
        unsupportedFeatures = Seq("webhooks: not converted"),
      )
      val summary = report.briefSummary
      summary must include("1 unmapped fields")
      summary must include("1 unsupported features")
      summary must startWith("Imported from OpenAPI. Conversion issues:")
    }

    "omit zero-count categories" in {
      val report = emptyReport.copy(fields = Seq(fieldReport("Foo", "bar", None)))
      report.briefSummary must not include "defaulted"
      report.briefSummary must not include "ignored"
      report.briefSummary must not include "path issues"
    }
  }
}
