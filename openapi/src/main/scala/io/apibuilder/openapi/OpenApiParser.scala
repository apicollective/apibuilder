package io.apibuilder.openapi

import io.circe.{parser => circeParser}
import io.circe.yaml.{parser => yamlParser}
import sttp.apispec.openapi.OpenAPI
import sttp.apispec.openapi.circe._

import java.nio.file.{Files, Path}
import scala.io.Source
import scala.util.Using

object OpenApiParser {

  def fromFile(path: Path): Either[String, OpenAPI] =
    Using(Source.fromFile(path.toFile, "UTF-8"))(_.mkString).toEither
      .left.map(e => s"${e.getClass.getSimpleName}: ${e.getMessage}")
      .flatMap(fromString)

  def fromUrl(url: String): Either[String, OpenAPI] =
    Using(Source.fromURL(url, "UTF-8"))(_.mkString).toEither
      .left.map(e => s"${e.getClass.getSimpleName}: ${e.getMessage}")
      .flatMap(fromString)

  def fromString(input: String): Either[String, OpenAPI] = {
    val trimmed = input.trim
    if (looksLikeYaml(trimmed))
      fromYaml(trimmed).left.map(e => s"Failed to parse input as YAML: $e")
    else
      fromJson(trimmed).left.map(e => s"Failed to parse input as JSON: $e")
  }

  def fromResource(resourcePath: String): Either[String, OpenAPI] = {
    val stream = Option(getClass.getClassLoader.getResourceAsStream(resourcePath))
      .toRight(s"Resource not found: $resourcePath")
    stream.flatMap { is =>
      Using(Source.fromInputStream(is, "UTF-8"))(_.mkString).toEither
        .left.map(_.getMessage)
        .flatMap(fromString)
    }
  }

  def fromJson(jsonString: String): Either[String, OpenAPI] =
    circeParser
      .parse(jsonString)
      .flatMap(_.as[OpenAPI])
      .left
      .map(_.getMessage)

  def fromYaml(yamlString: String): Either[String, OpenAPI] =
    yamlParser
      .parse(yamlString)
      .flatMap(_.as[OpenAPI])
      .left
      .map(_.getMessage)

  private def looksLikeYaml(input: String): Boolean =
    !input.startsWith("{") && !input.startsWith("[")
}
