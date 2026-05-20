package io.apibuilder.openapi

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.apispec.SecurityScheme
import sttp.apispec.openapi.Reference

import scala.collection.immutable.ListMap

class SecurityConverterSpec extends AnyWordSpec with Matchers {

  "SecurityConverter" must {

    "convert apiKey with in=header" in {
      val schemes = ListMap(
        "api_key" -> Right(SecurityScheme(`type` = "apiKey", name = Some("X-Api-Key"), in = Some("header"))),
      )
      val headers = SecurityConverter.convertSecuritySchemes(schemes)
      headers must have size 1
      headers.head.name must be("X-Api-Key")
      headers.head.`type` must be("string")
      headers.head.required must be(true)
    }

    "convert apiKey with description" in {
      val schemes = ListMap(
        "api_key" -> Right(
          SecurityScheme(
            `type` = "apiKey",
            name = Some("X-Api-Key"),
            in = Some("header"),
            description = Some("Your API key"),
          ),
        ),
      )
      val headers = SecurityConverter.convertSecuritySchemes(schemes)
      headers.head.description must be(Some("Your API key"))
    }

    "skip apiKey with in=query" in {
      val schemes = ListMap(
        "api_key" -> Right(SecurityScheme(`type` = "apiKey", name = Some("api_key"), in = Some("query"))),
      )
      SecurityConverter.convertSecuritySchemes(schemes) must be(empty)
    }

    "skip apiKey with in=cookie" in {
      val schemes = ListMap(
        "api_key" -> Right(SecurityScheme(`type` = "apiKey", name = Some("session"), in = Some("cookie"))),
      )
      SecurityConverter.convertSecuritySchemes(schemes) must be(empty)
    }

    "convert http bearer" in {
      val schemes = ListMap(
        "bearer" -> Right(SecurityScheme(`type` = "http", scheme = Some("bearer"))),
      )
      val headers = SecurityConverter.convertSecuritySchemes(schemes)
      headers must have size 1
      headers.head.name must be("Authorization")
      headers.head.`type` must be("string")
      headers.head.required must be(true)
      headers.head.description must be(Some("Bearer token"))
    }

    "convert http basic" in {
      val schemes = ListMap(
        "basic" -> Right(SecurityScheme(`type` = "http", scheme = Some("basic"))),
      )
      val headers = SecurityConverter.convertSecuritySchemes(schemes)
      headers must have size 1
      headers.head.name must be("Authorization")
      headers.head.description must be(Some("Basic authentication"))
    }

    "use explicit description over default for http scheme" in {
      val schemes = ListMap(
        "bearer" -> Right(
          SecurityScheme(`type` = "http", scheme = Some("bearer"), description = Some("JWT access token")),
        ),
      )
      SecurityConverter.convertSecuritySchemes(schemes).head.description must be(Some("JWT access token"))
    }

    "deduplicate Authorization headers from multiple http schemes" in {
      val schemes = ListMap(
        "bearer" -> Right(SecurityScheme(`type` = "http", scheme = Some("bearer"))),
        "basic" -> Right(SecurityScheme(`type` = "http", scheme = Some("basic"))),
      )
      val headers = SecurityConverter.convertSecuritySchemes(schemes)
      headers must have size 1
      headers.head.name must be("Authorization")
    }

    "skip oauth2" in {
      val schemes = ListMap("oauth" -> Right(SecurityScheme(`type` = "oauth2")))
      SecurityConverter.convertSecuritySchemes(schemes) must be(empty)
    }

    "skip openIdConnect" in {
      val schemes = ListMap("oidc" -> Right(SecurityScheme(`type` = "openIdConnect")))
      SecurityConverter.convertSecuritySchemes(schemes) must be(empty)
    }

    "skip references" in {
      val schemes = ListMap("ref" -> Left(Reference("#/components/securitySchemes/other")))
      SecurityConverter.convertSecuritySchemes(schemes) must be(empty)
    }

    "convert mixed schemes keeping only convertible ones" in {
      val schemes = ListMap(
        "api_key" -> Right(SecurityScheme(`type` = "apiKey", name = Some("X-Api-Key"), in = Some("header"))),
        "bearer" -> Right(SecurityScheme(`type` = "http", scheme = Some("bearer"))),
        "oauth" -> Right(SecurityScheme(`type` = "oauth2")),
        "query_key" -> Right(SecurityScheme(`type` = "apiKey", name = Some("key"), in = Some("query"))),
      )
      val headers = SecurityConverter.convertSecuritySchemes(schemes)
      headers must have size 2
      headers.map(_.name).toSet must be(Set("X-Api-Key", "Authorization"))
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

    "return false for oauth2" in {
      SecurityConverter.isConvertible(SecurityScheme(`type` = "oauth2")) must be(false)
    }

    "return false for openIdConnect" in {
      SecurityConverter.isConvertible(SecurityScheme(`type` = "openIdConnect")) must be(false)
    }
  }
}
