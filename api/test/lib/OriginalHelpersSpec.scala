package lib

import io.apibuilder.api.v0.models.{Original, OriginalForm, OriginalType}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

class OriginalHelpersSpec extends PlaySpec with OneAppPerSuite {

  "original" in {
    val data = TestHelper.readFile("../spec/apibuilder-api.json")
    OriginalUtil.toOriginal(OriginalForm(data = data)) must be(
      Original(
        OriginalType.ApiJson,
        data
      )
    )
  }

  "guessType" must {

    "apiJson" in {
      OriginalUtil.guessType(TestHelper.readFile("../spec/apibuilder-api.json")) must be(Some(OriginalType.ApiJson))
      OriginalUtil.guessType(TestHelper.readFile("../spec/apibuilder-spec.json")) must be(Some(OriginalType.ApiJson))
    }

    "serviceJson" in {
      OriginalUtil.guessType(TestHelper.readFile("../core/src/test/resources/apibuilder-service.json")) must be(Some(OriginalType.ServiceJson))
    }

    "swaggerJson" in {
      OriginalUtil.guessType(TestHelper.readFile("../swagger/src/test/resources/petstore-external-docs-example-security.json")) must be(Some(OriginalType.Swagger))
    }

    "avroIdl" in {
      OriginalUtil.guessType("  @namespace  ") must be(Some(OriginalType.AvroIdl))
      OriginalUtil.guessType("  protocol bar {}  ") must be(Some(OriginalType.AvroIdl))
    }

    "unknown" in {
      OriginalUtil.guessType("   ") must be(None)
    }

    "poorly formatted json" in {
      OriginalUtil.guessType("{   ") must be(None)
    }

  }

}
