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

    it("avroIdl") {
      OriginalUtil.guessType("  @namespace  ") should be(Some(OriginalType.AvroIdl))
      OriginalUtil.guessType("  protocol bar {}  ") should be(Some(OriginalType.AvroIdl))
    }

    describe("unknown") {
      OriginalUtil.guessType("   ") should be(None)
    }

  }

}
