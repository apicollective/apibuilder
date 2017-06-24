package lib

import io.apibuilder.apidoc.api.v0.models.{Original, OriginalForm, OriginalType}
import org.scalatest.{FunSpec, ShouldMatchers}

class OriginalUtilSpec extends FunSpec with ShouldMatchers {

  it("original") {
    val data = TestHelper.readFile("../spec/apidoc-api.json")
    OriginalUtil.toOriginal(OriginalForm(data = data)) should be(
      Original(
        OriginalType.ApiJson,
        data
      )
    )
  }

  describe("guessType") {

    it("apiJson") {
      OriginalUtil.guessType(TestHelper.readFile("../spec/apidoc-api.json")) should be(Some(OriginalType.ApiJson))
      OriginalUtil.guessType(TestHelper.readFile("../spec/apidoc-spec.json")) should be(Some(OriginalType.ApiJson))
    }

    it("serviceJson") {
      OriginalUtil.guessType(TestHelper.readFile("../core/src/test/resources/apidoc-service.json")) should be(Some(OriginalType.ServiceJson))
    }

    it("swaggerJson") {
      OriginalUtil.guessType(TestHelper.readFile("../swagger/src/test/resources/petstore-external-docs-example-security.json")) should be(Some(OriginalType.Swagger))
    }

    it("avroIdl") {
      OriginalUtil.guessType("  @namespace  ") should be(Some(OriginalType.AvroIdl))
      OriginalUtil.guessType("  protocol bar {}  ") should be(Some(OriginalType.AvroIdl))
    }

    it("unknown") {
      OriginalUtil.guessType("   ") should be(None)
    }

    it("poorly formatted json") {
      OriginalUtil.guessType("{   ") should be(None)
    }

  }

}
