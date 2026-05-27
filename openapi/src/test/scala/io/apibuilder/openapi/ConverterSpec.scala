package io.apibuilder.openapi

import lib.ServiceConfiguration
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.apispec.{Schema, SchemaType, SecurityScheme}
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
      service.models.map(_.name) must contain("money")
      service.resources must not be empty
    }

    "convert fedex-track" in {
      val openApi = OpenApiParser.fromResource("fedex-track.json").toOption.get
      val service = Converter.convert(openApi, testConfig)

      service.models must not be empty
      service.models.map(_.name) must contain("tracking_info")
      service.resources must not be empty
    }

    "convert fedex-eei-filing" in {
      val openApi = OpenApiParser.fromResource("fedex-eei-filing.json").toOption.get
      val service = Converter.convert(openApi, testConfig)

      service.models must not be empty
      service.models.map(_.name) must contain("gtic_response_vo")
      service.resources must not be empty
    }

    "convert security schemes to headers: apiKey and http produce distinct headers; oauth2 is deduped" in {
      val openApi = OpenAPI(
        info = Info("test", "1.0"),
        components = Some(
          Components(
            securitySchemes = ListMap(
              "api_key" -> Right(SecurityScheme(`type` = "apiKey", name = Some("X-Api-Key"), in = Some("header"))),
              "bearer"  -> Right(SecurityScheme(`type` = "http", scheme = Some("bearer"))),
              "oauth"   -> Right(SecurityScheme(`type` = "oauth2")),
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

    "oauth2 scheme emits degraded note in openapi_conversion attribute" in {
      val openApi = OpenAPI(
        info = Info("test", "1.0"),
        components = Some(
          Components(
            securitySchemes = ListMap(
              "oauth" -> Right(SecurityScheme(`type` = "oauth2")),
            ),
          ),
        ),
      )
      val service = Converter.convert(openApi, testConfig)

      val attr = service.attributes.find(_.name == "openapi_conversion").get
      attr.value.toString must include("oauth2")
    }

    "filterHeaders: header parameter is excluded from converted operations" in {
      val headerParam = Parameter(
        name = "X-Trace-Id",
        in = ParameterIn.Header,
        schema = Some(Schema(`type` = Some(List(SchemaType.String)))),
      )
      val operation = Operation(
        parameters = List(Right(headerParam)),
        responses = Responses(
          responses = ListMap(
            ResponsesCodeKey(200) -> Right(
              Response(
                description = "ok",
                content = ListMap(
                  "application/json" -> MediaType(
                    schema = Some(Schema($ref = Some("#/components/schemas/Widget"))),
                  ),
                ),
              ),
            ),
          ),
        ),
      )
      val openApi = OpenAPI(
        info = Info("test", "1.0"),
        paths = Paths(pathItems = ListMap("/widgets" -> PathItem(get = Some(operation)))),
      )

      val service = Converter.convert(openApi, testConfig, filterHeaders = Set("X-Trace-Id"))

      val params = service.resources.flatMap(_.operations).flatMap(_.parameters)
      params.exists(_.name == "X-Trace-Id") must be(false)
    }

    "set org key and namespace from ServiceConfiguration" in {
      val openApi = OpenAPI(info = Info("My Service", "1.0"))
      val config = ServiceConfiguration(orgKey = "myorg", orgNamespace = "io.myorg", version = "1.2.3")
      val service = Converter.convert(openApi, config)

      service.organization.key must be("myorg")
      service.namespace must startWith("io.myorg")
      service.version must be("1.2.3")
    }

    "description: appends brief summary to existing description" in {
      val openApi = OpenAPI(info = Info("My Service", "1.0", description = Some("Original description.")))
      val service = Converter.convert(openApi, testConfig)

      service.description must be(Symbol("defined"))
      service.description.get must startWith("Original description.")
      service.description.get must include("Imported from OpenAPI.")
    }

    "description: contains only brief summary when no original description" in {
      val openApi = OpenAPI(info = Info("My Service", "1.0"))
      val service = Converter.convert(openApi, testConfig)

      service.description must be(Some("Imported from OpenAPI."))
    }

    "attributes: includes openapi_conversion attribute" in {
      val openApi = OpenAPI(info = Info("My Service", "1.0"))
      val service = Converter.convert(openApi, testConfig)

      service.attributes must have size 1
      service.attributes.head.name must be("openapi_conversion")
    }

    "attributes: openapi_conversion value contains expected keys" in {
      val openApi = OpenAPI(info = Info("My Service", "1.0"))
      val service = Converter.convert(openApi, testConfig)

      val attrValue = service.attributes.head.value
      (attrValue \ "unmapped_fields").isDefined must be(true)
      (attrValue \ "defaulted_fields").isDefined must be(true)
      (attrValue \ "ignored_formats").isDefined must be(true)
      (attrValue \ "path_issues").isDefined must be(true)
      (attrValue \ "unsupported_features").isDefined must be(true)
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
