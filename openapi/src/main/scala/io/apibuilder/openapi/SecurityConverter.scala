package io.apibuilder.openapi

import io.apibuilder.spec.v0.models.Header
import io.apibuilder.validation.ScalarType
import sttp.apispec.SecurityScheme
import sttp.apispec.openapi.Reference

import scala.collection.immutable.ListMap

object SecurityConverter {

  def convertSecuritySchemes(
    schemes: ListMap[String, Either[Reference, SecurityScheme]],
  ): Seq[Header] = {
    schemes.toSeq
      .flatMap { case (_, schemeOrRef) =>
        schemeOrRef match {
          case Right(scheme) => convertScheme(scheme)
          case Left(_) => None
        }
      }
      .distinctBy(_.name)
  }

  private def convertScheme(scheme: SecurityScheme): Option[Header] = {
    scheme.`type` match {
      case "apiKey" if scheme.in.contains("header") =>
        scheme.name.map(n => makeHeader(n, scheme.description))

      case "http" =>
        val desc = scheme.description.orElse {
          scheme.scheme.map {
            case "bearer" => "Bearer token"
            case "basic" => "Basic authentication"
            case other => s"HTTP $other authentication"
          }
        }
        Some(makeHeader("Authorization", desc))

      case _ => None
    }
  }

  private def makeHeader(name: String, description: Option[String]): Header =
    Header(
      name = name,
      `type` = ScalarType.StringType.name,
      description = description,
      deprecation = None,
      required = true,
      default = None,
      attributes = Seq.empty,
    )

  def isConvertible(scheme: SecurityScheme): Boolean =
    scheme.`type` match {
      case "apiKey" => scheme.in.contains("header")
      case "http" => true
      case _ => false
    }
}
