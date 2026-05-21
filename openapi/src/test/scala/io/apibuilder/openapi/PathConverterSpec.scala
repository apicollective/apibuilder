package io.apibuilder.openapi

import io.apibuilder.spec.v0.{models => ab}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.apispec.openapi._
import sttp.apispec.{Schema, SchemaType}

import scala.collection.immutable.ListMap

class PathConverterSpec extends AnyWordSpec with Matchers {

  private def makeConverter(filterHeaders: Set[String] = Set.empty): PathConverter =
    new PathConverter(Map.empty, NamingConfig(), filterHeaders)

  private def makeResponse(schemaName: String): Response =
    Response(
      description = "ok",
      content = ListMap(
        "application/json" -> MediaType(
          schema = Some(Schema($ref = Some(s"#/components/schemas/$schemaName"))),
        ),
      ),
    )

  private def makePathsWith200(
    path: String,
    pathParams: List[Parameter] = Nil,
    opParams: List[Parameter] = Nil,
  ): Paths = {
    val operation = Operation(
      parameters = opParams.map(Right(_)),
      responses = Responses(
        responses = ListMap(ResponsesCodeKey(200) -> Right(makeResponse("Widget"))),
      ),
    )
    val pathItem = PathItem(
      get = Some(operation),
      parameters = pathParams.map(Right(_)),
    )
    Paths(pathItems = ListMap(path -> pathItem))
  }

  "PathConverter" must {

    "rewrite {id} path parameters to :id" in {
      val paths = makePathsWith200("/widgets/{id}")
      val result = makeConverter().convertPaths(paths)

      result.resources must not be empty
      val ops = result.resources.head.operations
      ops must not be empty
      ops.head.path must be("/widgets/:id")
    }

    "mergeParameters: operation-level parameter overrides path-level with same (name, in)" in {
      val pathParam = Parameter(
        name = "limit",
        in = ParameterIn.Query,
        description = Some("path-level"),
        schema = Some(Schema(`type` = Some(List(SchemaType.Integer)))),
      )
      val opParam = Parameter(
        name = "limit",
        in = ParameterIn.Query,
        description = Some("op-level"),
        schema = Some(Schema(`type` = Some(List(SchemaType.Integer)))),
      )
      val paths = makePathsWith200("/widgets", pathParams = List(pathParam), opParams = List(opParam))
      val result = makeConverter().convertPaths(paths)

      val params = result.resources.head.operations.head.parameters
      params.count(_.name == "limit") must be(1)
      params.find(_.name == "limit").flatMap(_.description) must be(Some("op-level"))
    }

    "filterHeaders: header parameter matching filter is excluded" in {
      val headerParam = Parameter(
        name = "X-Api-Key",
        in = ParameterIn.Header,
        schema = Some(Schema(`type` = Some(List(SchemaType.String)))),
      )
      val paths = makePathsWith200("/widgets", opParams = List(headerParam))

      val withFilter = new PathConverter(Map.empty, NamingConfig(), filterHeaders = Set("X-Api-Key"))
      val result = withFilter.convertPaths(paths)

      val params = result.resources.head.operations.head.parameters
      params.exists(_.name == "X-Api-Key") must be(false)
    }

    "filterHeaders: matching is case-insensitive" in {
      val headerParam = Parameter(
        name = "x-api-key",
        in = ParameterIn.Header,
        schema = Some(Schema(`type` = Some(List(SchemaType.String)))),
      )
      val paths = makePathsWith200("/widgets", opParams = List(headerParam))

      val withFilter = new PathConverter(Map.empty, NamingConfig(), filterHeaders = Set("X-API-KEY"))
      val result = withFilter.convertPaths(paths)

      val params = result.resources.head.operations.head.parameters
      params.exists(_.name == "x-api-key") must be(false)
    }

    "no typed response: spec with only 204 responses produces no resources" in {
      val operation = Operation(
        responses = Responses(
          responses = ListMap(ResponsesCodeKey(204) -> Right(Response(description = "no content"))),
        ),
      )
      val pathItem = PathItem(delete = Some(operation))
      val paths = Paths(pathItems = ListMap("/widgets/{id}" -> pathItem))

      val result = makeConverter().convertPaths(paths)
      result.resources must be(empty)
    }

    "non-JSON response with no JSON alternative reports one consolidated issue" in {
      val operation = Operation(
        responses = Responses(responses = ListMap(
          ResponsesCodeKey(200) -> Right(Response(
            description = "video",
            content = ListMap("video/mp4" -> MediaType()),
          )),
        )),
      )
      val paths = Paths(pathItems = ListMap("/videos/:id/content" -> PathItem(get = Some(operation))))
      val report = makeConverter().convertPaths(paths).pathReports.head

      report.unsupported.count(_.contains("no JSON content")) must be(1)
      report.unsupported.find(_.contains("no JSON content")).get must include("video/mp4")
    }

    "non-JSON response with multiple content types consolidates into one issue" in {
      val operation = Operation(
        responses = Responses(responses = ListMap(
          ResponsesCodeKey(200) -> Right(Response(
            description = "media",
            content = ListMap("video/mp4" -> MediaType(), "image/webp" -> MediaType()),
          )),
        )),
      )
      val paths = Paths(pathItems = ListMap("/videos/:id/content" -> PathItem(get = Some(operation))))
      val report = makeConverter().convertPaths(paths).pathReports.head

      val contentIssues = report.unsupported.filter(_.contains("no JSON content"))
      contentIssues must have size 1
      contentIssues.head must include("video/mp4")
      contentIssues.head must include("image/webp")
    }

    "response with both JSON and non-JSON content types reports no content-type issue" in {
      val operation = Operation(
        responses = Responses(responses = ListMap(
          ResponsesCodeKey(200) -> Right(Response(
            description = "ok",
            content = ListMap(
              "application/json" -> MediaType(schema = Some(Schema($ref = Some("#/components/schemas/Widget")))),
              "text/event-stream" -> MediaType(),
            ),
          )),
        )),
      )
      val paths = Paths(pathItems = ListMap("/widgets" -> PathItem(get = Some(operation))))
      val report = makeConverter().convertPaths(paths).pathReports.head

      report.unsupported.filter(_.contains("no JSON content")) must be(empty)
    }

    "non-JSON request body with no JSON alternative reports one consolidated issue" in {
      val operation = Operation(
        requestBody = Some(Right(RequestBody(
          content = ListMap("multipart/form-data" -> MediaType()),
        ))),
        responses = Responses(responses = ListMap(
          ResponsesCodeKey(200) -> Right(makeResponse("Widget")),
        )),
      )
      val paths = Paths(pathItems = ListMap("/uploads" -> PathItem(post = Some(operation))))
      val report = makeConverter().convertPaths(paths).pathReports.head

      report.unsupported.count(_.contains("no JSON request body")) must be(1)
      report.unsupported.find(_.contains("no JSON request body")).get must include("multipart/form-data")
    }

    "multipart/form-data with $ref schema extracts body type" in {
      val operation = Operation(
        requestBody = Some(Right(RequestBody(
          content = ListMap("multipart/form-data" -> MediaType(
            schema = Some(Schema($ref = Some("#/components/schemas/UploadRequest"))),
          )),
        ))),
        responses = Responses(responses = ListMap(
          ResponsesCodeKey(200) -> Right(makeResponse("Widget")),
        )),
      )
      val paths = Paths(pathItems = ListMap("/uploads" -> PathItem(post = Some(operation))))
      val result = makeConverter().convertPaths(paths)

      val body = result.resources.head.operations.head.body
      body.map(_.`type`) must be(Some("upload_request"))
    }

    "multipart/form-data with $ref schema suppresses the non-JSON body warning" in {
      val operation = Operation(
        requestBody = Some(Right(RequestBody(
          content = ListMap("multipart/form-data" -> MediaType(
            schema = Some(Schema($ref = Some("#/components/schemas/UploadRequest"))),
          )),
        ))),
        responses = Responses(responses = ListMap(
          ResponsesCodeKey(200) -> Right(makeResponse("Widget")),
        )),
      )
      val paths = Paths(pathItems = ListMap("/uploads" -> PathItem(post = Some(operation))))
      val report = makeConverter().convertPaths(paths).pathReports.head

      report.unsupported.filter(_.contains("no JSON request body")) must be(empty)
    }

    "multipart/form-data with inline object schema produces no body" in {
      val operation = Operation(
        requestBody = Some(Right(RequestBody(
          content = ListMap("multipart/form-data" -> MediaType(
            schema = Some(Schema(`type` = Some(List(SchemaType.Object)))),
          )),
        ))),
        responses = Responses(responses = ListMap(
          ResponsesCodeKey(200) -> Right(makeResponse("Widget")),
        )),
      )
      val paths = Paths(pathItems = ListMap("/uploads" -> PathItem(post = Some(operation))))
      val result = makeConverter().convertPaths(paths)

      result.resources.head.operations.head.body must be(None)
    }

    "application/x-www-form-urlencoded with $ref schema extracts body type" in {
      val operation = Operation(
        requestBody = Some(Right(RequestBody(
          content = ListMap("application/x-www-form-urlencoded" -> MediaType(
            schema = Some(Schema($ref = Some("#/components/schemas/FormRequest"))),
          )),
        ))),
        responses = Responses(responses = ListMap(
          ResponsesCodeKey(200) -> Right(makeResponse("Widget")),
        )),
      )
      val paths = Paths(pathItems = ListMap("/submit" -> PathItem(post = Some(operation))))
      val result = makeConverter().convertPaths(paths)

      val body = result.resources.head.operations.head.body
      body.map(_.`type`) must be(Some("form_request"))
    }

    "request body with both JSON and non-JSON content types reports no body issue" in {
      val operation = Operation(
        requestBody = Some(Right(RequestBody(
          content = ListMap(
            "application/json" -> MediaType(schema = Some(Schema($ref = Some("#/components/schemas/Widget")))),
            "multipart/form-data" -> MediaType(),
          ),
        ))),
        responses = Responses(responses = ListMap(
          ResponsesCodeKey(200) -> Right(makeResponse("Widget")),
        )),
      )
      val paths = Paths(pathItems = ListMap("/widgets" -> PathItem(post = Some(operation))))
      val report = makeConverter().convertPaths(paths).pathReports.head

      report.unsupported.filter(_.contains("no JSON request body")) must be(empty)
    }

    "response key is formatted as numeric code not as case class" in {
      val operation = Operation(
        responses = Responses(responses = ListMap(
          ResponsesCodeKey(200) -> Right(Response(
            description = "video",
            content = ListMap("video/mp4" -> MediaType()),
          )),
        )),
      )
      val paths = Paths(pathItems = ListMap("/videos/:id" -> PathItem(get = Some(operation))))
      val report = makeConverter().convertPaths(paths).pathReports.head

      report.unsupported.exists(_.contains("ResponsesCodeKey")) must be(false)
      report.unsupported.exists(_.contains("response 200:")) must be(true)
    }
  }
}
