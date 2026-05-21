package io.apibuilder.openapi

import io.apibuilder.spec.v0.models.Header
import io.apibuilder.validation.ScalarType
import sttp.apispec.{OAuthFlows, SecurityScheme}
import sttp.apispec.openapi.Reference

import scala.collection.immutable.ListMap

case class SecurityConversionResult(
  headers: Seq[Header],
  degradedNotes: Seq[String],
)

object SecurityConverter {

  def convertSecuritySchemes(
    schemes: ListMap[String, Either[Reference, SecurityScheme]],
  ): SecurityConversionResult = {
    val results = schemes.toSeq.flatMap { case (name, schemeOrRef) =>
      schemeOrRef match {
        case Right(scheme) => convertScheme(name, scheme)
        case Left(_) => None
      }
    }
    SecurityConversionResult(
      headers = results.map(_._1).distinctBy(_.name),
      degradedNotes = results.flatMap(_._2),
    )
  }

  private def convertScheme(name: String, scheme: SecurityScheme): Option[(Header, Option[String])] =
    scheme.`type` match {
      case "apiKey" if scheme.in.contains("header") =>
        scheme.name.map(n => (makeHeader(n, scheme.description), None))

      case "http" =>
        val desc = scheme.description.orElse {
          scheme.scheme.map {
            case "bearer" => "Bearer token"
            case "basic"  => "Basic authentication"
            case other    => s"HTTP $other authentication"
          }
        }
        Some((makeHeader("Authorization", desc), None))

      case "oauth2" =>
        val flowDesc = oauthFlowDescription(scheme.flows)
        val desc = Seq(scheme.description, flowDesc).flatten match {
          case Nil   => Some("OAuth2 authentication")
          case parts => Some(parts.mkString(". "))
        }
        val note = s"securityScheme '$name': oauth2 converted to Authorization header; flow details not representable in apibuilder"
        Some((makeHeader("Authorization", desc), Some(note)))

      case "openIdConnect" =>
        val discoveryDesc = scheme.openIdConnectUrl.map(u => s"OpenID Connect discovery: $u")
        val desc = Seq(scheme.description, discoveryDesc).flatten match {
          case Nil   => Some("OpenID Connect authentication")
          case parts => Some(parts.mkString(". "))
        }
        val note = s"securityScheme '$name': openIdConnect converted to Authorization header; discovery URL not representable in apibuilder"
        Some((makeHeader("Authorization", desc), Some(note)))

      case _ => None
    }

  private def oauthFlowDescription(flowsOpt: Option[OAuthFlows]): Option[String] =
    flowsOpt.map { flows =>
      val parts = Seq(
        flows.`implicit`.map(f =>
          s"implicit (authorizationUrl: ${f.authorizationUrl.getOrElse("?")}; scopes: ${f.scopes.keys.mkString(", ")})"
        ),
        flows.password.map(f =>
          s"password (tokenUrl: ${f.tokenUrl.getOrElse("?")}; scopes: ${f.scopes.keys.mkString(", ")})"
        ),
        flows.clientCredentials.map(f =>
          s"clientCredentials (tokenUrl: ${f.tokenUrl.getOrElse("?")}; scopes: ${f.scopes.keys.mkString(", ")})"
        ),
        flows.authorizationCode.map(f =>
          s"authorizationCode (authorizationUrl: ${f.authorizationUrl.getOrElse("?")}; tokenUrl: ${f.tokenUrl.getOrElse("?")}; scopes: ${f.scopes.keys.mkString(", ")})"
        ),
      ).flatten
      if (parts.isEmpty) "OAuth2" else s"OAuth2 flows: ${parts.mkString("; ")}"
    }

  def isConvertible(scheme: SecurityScheme): Boolean = scheme.`type` match {
    case "apiKey"        => scheme.in.contains("header")
    case "http"          => true
    case "oauth2"        => true
    case "openIdConnect" => true
    case _               => false
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
}
