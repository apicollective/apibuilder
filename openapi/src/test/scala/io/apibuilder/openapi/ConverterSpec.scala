package io.apibuilder.openapi

import lib.ServiceConfiguration
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.apispec.SecurityScheme
import sttp.apispec.openapi.{Components, Info, _}

import scala.collection.immutable.ListMap

class ConverterSpec extends AnyWordSpec with Matchers {

  private val testConfig = ServiceConfiguration(orgKey = "test", orgNamespace = "io.test", version = "0.0.1")

  "OpenApiParser" must {
    "parse fedex-ship-api.json" in {
      val result = OpenApiParser.fromResource("fedex-ship-api.json")
      result must be(Symbol("right"))
      val openApi = result.toOption.get
      openApi.info.title must not be empty
      openApi.components must be(Symbol("defined"))
      openApi.components.get.schemas must not be empty
      openApi.paths.pathItems must not be empty
    }

    "parse fedex-track.json" in {
      OpenApiParser.fromResource("fedex-track.json") must be(Symbol("right"))
    }

    "parse fedex-eei-filing.json" in {
      OpenApiParser.fromResource("fedex-eei-filing.json") must be(Symbol("right"))
    }
  }

  "Converter" must {
    "convert fedex-ship-api" in {
      val openApi = OpenApiParser.fromResource("fedex-ship-api.json").toOption.get
      val service = Converter.convert(openApi, testConfig)

      service.models must not be empty
      service.resources must not be empty
    }

    "convert fedex-track" in {
      val openApi = OpenApiParser.fromResource("fedex-track.json").toOption.get
      val service = Converter.convert(openApi, testConfig)
      service.models must not be empty
    }

    "convert fedex-eei-filing" in {
      val openApi = OpenApiParser.fromResource("fedex-eei-filing.json").toOption.get
      val service = Converter.convert(openApi, testConfig)
      service.models must not be empty
    }

    "convert security schemes to headers" in {
      val openApi = OpenAPI(
        info = Info("test", "1.0"),
        components = Some(
          Components(
            securitySchemes = ListMap(
              "api_key" -> Right(
                SecurityScheme(`type` = "apiKey", name = Some("X-Api-Key"), in = Some("header")),
              ),
              "bearer" -> Right(SecurityScheme(`type` = "http", scheme = Some("bearer"))),
              "oauth" -> Right(SecurityScheme(`type` = "oauth2")),
            ),
          ),
        ),
      )
      val service = Converter.convert(openApi, testConfig)

      service.headers must have size 2
      service.headers.map(_.name).toSet must be(Set("X-Api-Key", "Authorization"))
      service.headers.foreach { h =>
        h.`type` must be("string")
        h.required must be(true)
      }
    }

    "set org key and namespace from ServiceConfiguration" in {
      val openApi = OpenAPI(info = Info("My Service", "1.0"))
      val config = ServiceConfiguration(orgKey = "myorg", orgNamespace = "io.myorg", version = "1.2.3")
      val service = Converter.convert(openApi, config)

      service.organization.key must be("myorg")
      service.namespace must startWith("io.myorg")
      service.version must be("1.2.3")
    }
  }

  "OpenApiServiceValidator" must {
    "validate a valid OpenAPI 3 JSON spec" in {
      val spec = OpenApiParser.fromResource("fedex-ship-api.json").toOption.get
      import io.circe.syntax._
      import sttp.apispec.openapi.circe._
      val json = spec.asJson.noSpaces
      val result = OpenApiServiceValidator(testConfig).validate(json)
      result.isValid must be(true)
    }
  }
}
