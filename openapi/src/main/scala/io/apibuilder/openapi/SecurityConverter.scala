package io.apibuilder.openapi

import io.apibuilder.spec.v0.models.{Attribute, Header}
import io.apibuilder.validation.ScalarType
import play.api.libs.json.{JsObject, Json}
import sttp.apispec.{OAuthFlow, OAuthFlows, SecurityScheme}
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
        case Left(_)       => None
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
        val attr = oauth2Attribute(scheme.flows)
        val note = s"securityScheme '$name': oauth2 converted to Authorization header; flow details preserved in 'oauth2' attribute"
        Some((makeHeader("Authorization", desc, Seq(attr)), Some(note)))

      case "openIdConnect" =>
        val discoveryDesc = scheme.openIdConnectUrl.map(u => s"OpenID Connect discovery: $u")
        val desc = Seq(scheme.description, discoveryDesc).flatten match {
          case Nil   => Some("OpenID Connect authentication")
          case parts => Some(parts.mkString(". "))
        }
        val attr = oidcAttribute(scheme.openIdConnectUrl)
        val note = s"securityScheme '$name': openIdConnect converted to Authorization header; discovery URL preserved in 'openid_connect' attribute"
        Some((makeHeader("Authorization", desc, Seq(attr)), Some(note)))

      case _ => None
    }

  private def oauth2Attribute(flowsOpt: Option[OAuthFlows]): Attribute = {
    val flowsJson = flowsOpt.fold(Json.obj()) { flows =>
      Seq(
        flows.`implicit`.map(f => "implicit" -> flowJson(f, authUrl = true)),
        flows.password.map(f => "password" -> flowJson(f, authUrl = false)),
        flows.clientCredentials.map(f => "client_credentials" -> flowJson(f, authUrl = false)),
        flows.authorizationCode.map(f => "authorization_code" -> flowJson(f, authUrl = true)),
      ).flatten.foldLeft(Json.obj()) { case (acc, (k, v)) => acc + (k -> v) }
    }
    Attribute(name = "oauth2", value = Json.obj("flows" -> flowsJson))
  }

  private def flowJson(flow: OAuthFlow, authUrl: Boolean): JsObject = {
    val stringFields = Seq(
      Option.when(authUrl)(flow.authorizationUrl).flatten.map("authorization_url" -> _),
      flow.tokenUrl.map("token_url" -> _),
      flow.refreshUrl.map("refresh_url" -> _),
    ).flatten
    val base = stringFields.foldLeft(Json.obj()) { case (acc, (k, v)) => acc + (k -> Json.toJson(v)) }
    if (flow.scopes.nonEmpty) base + ("scopes" -> Json.toJson(flow.scopes.toMap)) else base
  }

  private def oidcAttribute(discoveryUrl: Option[String]): Attribute =
    Attribute(
      name = "openid_connect",
      value = discoveryUrl.fold(Json.obj())(u => Json.obj("discovery_url" -> u)),
    )

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

  private def makeHeader(name: String, description: Option[String], attributes: Seq[Attribute] = Seq.empty): Header =
    Header(
      name = name,
      `type` = ScalarType.StringType.name,
      description = description,
      deprecation = None,
      required = true,
      default = None,
      attributes = attributes,
    )
}
