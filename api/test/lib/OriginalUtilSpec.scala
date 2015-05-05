package lib

import com.gilt.apidoc.api.v0.models.{Original, OriginalForm, OriginalType}
import org.scalatest.{FunSpec, ShouldMatchers}

class OriginalUtilSpec extends FunSpec with ShouldMatchers {

  def readFile(path: String): String = {
    scala.io.Source.fromFile(path).getLines.mkString("\n")
  }

  it("original") {
    val data = readFile("../spec/api.json")
    OriginalUtil.toOriginal(OriginalForm(data = data)) should be(
      Original(
        OriginalType.ApiJson,
        data
      )
    )
  }

  describe("guessType") {

    it("apiJson") {
      OriginalUtil.guessType(readFile("../spec/api.json")) should be(Some(OriginalType.ApiJson))
      OriginalUtil.guessType(readFile("../spec/service.json")) should be(Some(OriginalType.ApiJson))
    }

    it("swaggerJson") {
      OriginalUtil.guessType(readFile("../swagger/src/test/resources/petstore-with-external-docs.json")) should be(Some(OriginalType.SwaggerJson))
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
