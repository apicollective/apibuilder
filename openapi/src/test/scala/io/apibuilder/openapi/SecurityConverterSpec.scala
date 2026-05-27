package io.apibuilder.openapi

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import sttp.apispec.{OAuthFlow, OAuthFlows, SecurityScheme}
import sttp.apispec.openapi.Reference

import scala.collection.immutable.ListMap

class SecurityConverterSpec extends AnyWordSpec with Matchers {

  private def convert(schemes: ListMap[String, Either[Reference, SecurityScheme]]): SecurityConversionResult =
    SecurityConverter.convertSecuritySchemes(schemes)

  "SecurityConverter" must {

    "convert apiKey with in=header" in {
      val result = convert(ListMap(
        "api_key" -> Right(SecurityScheme(`type` = "apiKey", name = Some("X-Api-Key"), in = Some("header"))),
      ))
      result.headers must have size 1
      result.headers.head.name must be("X-Api-Key")
      result.headers.head.`type` must be("string")
      result.headers.head.required must be(true)
      result.degradedNotes must be(empty)
    }

    "convert apiKey with description" in {
      val result = convert(ListMap(
        "api_key" -> Right(SecurityScheme(
          `type` = "apiKey", name = Some("X-Api-Key"), in = Some("header"),
          description = Some("Your API key"),
        )),
      ))
      result.headers.head.description must be(Some("Your API key"))
    }

    "skip apiKey with in=query" in {
      convert(ListMap(
        "api_key" -> Right(SecurityScheme(`type` = "apiKey", name = Some("api_key"), in = Some("query"))),
      )).headers must be(empty)
    }

    "skip apiKey with in=cookie" in {
      convert(ListMap(
        "api_key" -> Right(SecurityScheme(`type` = "apiKey", name = Some("session"), in = Some("cookie"))),
      )).headers must be(empty)
    }

    "convert http bearer" in {
      val result = convert(ListMap("bearer" -> Right(SecurityScheme(`type` = "http", scheme = Some("bearer")))))
      result.headers must have size 1
      result.headers.head.name must be("Authorization")
      result.headers.head.description must be(Some("Bearer token"))
      result.degradedNotes must be(empty)
    }

    "convert http basic" in {
      val result = convert(ListMap("basic" -> Right(SecurityScheme(`type` = "http", scheme = Some("basic")))))
      result.headers.head.name must be("Authorization")
      result.headers.head.description must be(Some("Basic authentication"))
    }

    "use explicit description over default for http scheme" in {
      val result = convert(ListMap(
        "bearer" -> Right(SecurityScheme(`type` = "http", scheme = Some("bearer"), description = Some("JWT access token"))),
      ))
      result.headers.head.description must be(Some("JWT access token"))
    }

    "deduplicate Authorization headers from multiple http schemes" in {
      val result = convert(ListMap(
        "bearer" -> Right(SecurityScheme(`type` = "http", scheme = Some("bearer"))),
        "basic"  -> Right(SecurityScheme(`type` = "http", scheme = Some("basic"))),
      ))
      result.headers must have size 1
      result.headers.head.name must be("Authorization")
    }

    "convert oauth2 to Authorization header" in {
      val result = convert(ListMap("oauth" -> Right(SecurityScheme(`type` = "oauth2"))))
      result.headers must have size 1
      result.headers.head.name must be("Authorization")
    }

    "convert oauth2 with clientCredentials flow: include flow details in description" in {
      val flows = OAuthFlows(clientCredentials = Some(OAuthFlow(
        tokenUrl = Some("https://auth.example.com/token"),
        scopes = ListMap("read:widgets" -> "Read widgets", "write:widgets" -> "Write widgets"),
      )))
      val result = convert(ListMap("oauth" -> Right(SecurityScheme(`type` = "oauth2", flows = Some(flows)))))
      val desc = result.headers.head.description.getOrElse("")
      desc must include("clientCredentials")
      desc must include("https://auth.example.com/token")
      desc must include("read:widgets")
    }

    "convert oauth2 with clientCredentials flow: persist flow details in oauth2 attribute" in {
      val flows = OAuthFlows(clientCredentials = Some(OAuthFlow(
        tokenUrl = Some("https://auth.example.com/token"),
        scopes = ListMap("read:widgets" -> "Read widgets", "write:widgets" -> "Write widgets"),
      )))
      val result = convert(ListMap("oauth" -> Right(SecurityScheme(`type` = "oauth2", flows = Some(flows)))))
      val attr = result.headers.head.attributes.find(_.name == "oauth2").get
      val attrStr = attr.value.toString
      attrStr must include("client_credentials")
      attrStr must include("https://auth.example.com/token")
      attrStr must include("read:widgets")
    }

    "convert oauth2 with authorizationCode flow: persists both URLs in attribute" in {
      val flows = OAuthFlows(authorizationCode = Some(OAuthFlow(
        authorizationUrl = Some("https://auth.example.com/authorize"),
        tokenUrl = Some("https://auth.example.com/token"),
        scopes = ListMap("openid" -> "OpenID"),
      )))
      val result = convert(ListMap("oauth" -> Right(SecurityScheme(`type` = "oauth2", flows = Some(flows)))))
      val attrStr = result.headers.head.attributes.find(_.name == "oauth2").get.value.toString
      attrStr must include("authorization_code")
      attrStr must include("authorization_url")
      attrStr must include("token_url")
    }

    "convert oauth2 with no flows: oauth2 attribute has empty flows object" in {
      val result = convert(ListMap("oauth" -> Right(SecurityScheme(`type` = "oauth2"))))
      val attr = result.headers.head.attributes.find(_.name == "oauth2").get
      attr.value must be(Json.obj("flows" -> Json.obj()))
    }

    "convert oauth2: emit degraded note referencing the attribute" in {
      val result = convert(ListMap("myOAuth" -> Right(SecurityScheme(`type` = "oauth2"))))
      result.degradedNotes must have size 1
      result.degradedNotes.head must include("myOAuth")
      result.degradedNotes.head must include("oauth2")
      result.degradedNotes.head must include("attribute")
    }

    "convert openIdConnect to Authorization header with discovery URL in description" in {
      val result = convert(ListMap(
        "oidc" -> Right(SecurityScheme(
          `type` = "openIdConnect",
          openIdConnectUrl = Some("https://auth.example.com/.well-known/openid-configuration"),
        )),
      ))
      result.headers must have size 1
      result.headers.head.name must be("Authorization")
      result.headers.head.description.getOrElse("") must include("https://auth.example.com/.well-known/openid-configuration")
    }

    "convert openIdConnect: persists discovery URL in openid_connect attribute" in {
      val result = convert(ListMap(
        "oidc" -> Right(SecurityScheme(
          `type` = "openIdConnect",
          openIdConnectUrl = Some("https://auth.example.com/.well-known/openid-configuration"),
        )),
      ))
      val attr = result.headers.head.attributes.find(_.name == "openid_connect").get
      attr.value.toString must include("https://auth.example.com/.well-known/openid-configuration")
    }

    "convert openIdConnect with no discovery URL: openid_connect attribute has empty object" in {
      val result = convert(ListMap("oidc" -> Right(SecurityScheme(`type` = "openIdConnect"))))
      val attr = result.headers.head.attributes.find(_.name == "openid_connect").get
      attr.value must be(Json.obj())
    }

    "convert openIdConnect: emit degraded note referencing the attribute" in {
      val result = convert(ListMap("myOidc" -> Right(SecurityScheme(`type` = "openIdConnect"))))
      result.degradedNotes must have size 1
      result.degradedNotes.head must include("myOidc")
      result.degradedNotes.head must include("openIdConnect")
      result.degradedNotes.head must include("attribute")
    }

    "skip references" in {
      convert(ListMap("ref" -> Left(Reference("#/components/securitySchemes/other")))).headers must be(empty)
    }

    "convert all scheme types to at most two distinct headers" in {
      val result = convert(ListMap(
        "api_key"   -> Right(SecurityScheme(`type` = "apiKey", name = Some("X-Api-Key"), in = Some("header"))),
        "bearer"    -> Right(SecurityScheme(`type` = "http", scheme = Some("bearer"))),
        "oauth"     -> Right(SecurityScheme(`type` = "oauth2")),
        "query_key" -> Right(SecurityScheme(`type` = "apiKey", name = Some("key"), in = Some("query"))),
      ))
      result.headers must have size 2
      result.headers.map(_.name).toSet must be(Set("X-Api-Key", "Authorization"))
    }
  }

  "isConvertible" must {

    "return true for apiKey with in=header" in {
      SecurityConverter.isConvertible(
        SecurityScheme(`type` = "apiKey", name = Some("X-Key"), in = Some("header")),
      ) must be(true)
    }

    "return false for apiKey with in=query" in {
      SecurityConverter.isConvertible(
        SecurityScheme(`type` = "apiKey", name = Some("key"), in = Some("query")),
      ) must be(false)
    }

    "return true for http" in {
      SecurityConverter.isConvertible(SecurityScheme(`type` = "http", scheme = Some("bearer"))) must be(true)
    }

    "return true for oauth2" in {
      SecurityConverter.isConvertible(SecurityScheme(`type` = "oauth2")) must be(true)
    }

    "return true for openIdConnect" in {
      SecurityConverter.isConvertible(SecurityScheme(`type` = "openIdConnect")) must be(true)
    }
  }
}
