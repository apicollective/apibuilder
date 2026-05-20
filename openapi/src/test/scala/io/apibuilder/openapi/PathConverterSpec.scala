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
  }
}
