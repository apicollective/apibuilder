package core

import _root_.builder.OriginalValidator
import lib.ServiceConfiguration
import com.bryzek.apidoc.api.v0.models.Original
import com.bryzek.apidoc.api.v0.models.OriginalType.SwaggerJson
import org.scalatest.{FunSpec, Matchers}

class OriginalValidatorSpec
    extends FunSpec
    with Matchers{

  private def readFile(path: String): String = {
    scala.io.Source.fromFile(path).getLines.mkString("\n")
  }

  val config = ServiceConfiguration(
    orgKey = "apidoc",
    orgNamespace = "me.apidoc",
    version = "0.0.2-dev"
  )

  describe("OriginalValidator") {

    it("should validate valid swagger json") {
      val filename = "refs.json"
      val path = s"swagger/src/test/resources/$filename"
      val result =
        OriginalValidator(
          config,
          original = Original (
            SwaggerJson,
            readFile(path)
          ),
          new MockServiceFetcher()
        ).validate

    result.isRight should
      be(true)
    }
  }
}
