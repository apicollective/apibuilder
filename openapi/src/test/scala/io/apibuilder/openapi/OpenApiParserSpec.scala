package io.apibuilder.openapi

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.nio.file.Paths

class OpenApiParserSpec extends AnyWordSpec with Matchers {

  private val minimalJsonSpec =
    """|{
       |  "openapi": "3.0.0",
       |  "info": { "title": "Test API", "version": "1.0.0" },
       |  "paths": {}
       |}""".stripMargin

  private val minimalYamlSpec =
    """|openapi: "3.0.0"
       |info:
       |  title: Test API
       |  version: "1.0.0"
       |paths: {}
       |""".stripMargin

  "OpenApiParser" must {

    "parse a valid minimal JSON OpenAPI string" in {
      val result = OpenApiParser.fromString(minimalJsonSpec)
      result must be(Symbol("right"))
      result.toOption.get.info.title must be("Test API")
    }

    "parse a valid minimal YAML OpenAPI string" in {
      val result = OpenApiParser.fromString(minimalYamlSpec)
      result must be(Symbol("right"))
      result.toOption.get.info.title must be("Test API")
    }

    "return Left for invalid JSON containing context message" in {
      val result = OpenApiParser.fromString("{ not valid json }")
      result must be(Symbol("left"))
      result.left.get must include("Failed to parse input as JSON")
    }

    "return Left for invalid YAML containing context message" in {
      val result = OpenApiParser.fromString(":\n  - bad: [unclosed")
      result must be(Symbol("left"))
      result.left.get must include("Failed to parse input as YAML")
    }

    "fromFile with non-existent path returns Left containing exception class name" in {
      val path = Paths.get("/no/such/file/does/not/exist.json")
      val result = OpenApiParser.fromFile(path)
      result must be(Symbol("left"))
      result.left.get must include("Exception")
    }
  }
}
