package lib

import io.apibuilder.api.v0.models.{Original, OriginalForm, OriginalType}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

class OriginalUtilSpec extends PlaySpec with OneAppPerSuite {

  it("original") {
    val data = TestHelper.readFile("../spec/apibuilder-api.json")
    OriginalUtil.toOriginal(OriginalForm(data = data)) must be(
      Original(
        OriginalType.ApiJson,
        data
      )
    )
  }

  describe("guessType") {

    it("apiJson") {
      OriginalUtil.guessType(TestHelper.readFile("../spec/apibuilder-api.json")) must be(Some(OriginalType.ApiJson))
      OriginalUtil.guessType(TestHelper.readFile("../spec/apibuilder-spec.json")) must be(Some(OriginalType.ApiJson))
    }

    it("serviceJson") {
      OriginalUtil.guessType(TestHelper.readFile("../core/src/test/resources/apibuilder-service.json")) must be(Some(OriginalType.ServiceJson))
    }

    it("swaggerJson") {
      OriginalUtil.guessType(TestHelper.readFile("../swagger/src/test/resources/petstore-external-docs-example-security.json")) must be(Some(OriginalType.Swagger))
    }

    it("avroIdl") {
      OriginalUtil.guessType("  @namespace  ") must be(Some(OriginalType.AvroIdl))
      OriginalUtil.guessType("  protocol bar {}  ") must be(Some(OriginalType.AvroIdl))
    }

    it("unknown") {
      OriginalUtil.guessType("   ") must be(None)
    }

    it("poorly formatted json") {
      OriginalUtil.guessType("{   ") must be(None)
    }

  }

}
