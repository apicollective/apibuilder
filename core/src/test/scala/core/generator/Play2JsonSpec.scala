package core.generator

import core._
import org.scalatest.{ ShouldMatchers, FunSpec }

class Play2JsonSpec extends FunSpec with ShouldMatchers {

  //private lazy val service = TestHelper.parseFile("reference-api/api.json").serviceDescription.get

  describe("quality schema") {

    lazy val quality = new ScalaServiceDescription(TestHelper.parseFile("core/src/test/resources/examples/quality.json").serviceDescription.get)
    lazy val play2Json = Play2Json(quality)

    describe("plan") {

      lazy val plan = quality.models.find(_.name == "Plan").get

      it("readers") {
        TestHelper.assertEqualsFile(
          "core/src/test/resources/generators/play-2-json-spec-quality-plan-readers.txt",
          play2Json.readers(plan)
        )
      }

      it("writers") {
        TestHelper.assertEqualsFile(
          "core/src/test/resources/generators/play-2-json-spec-quality-plan-writers.txt",
          play2Json.writers(plan)
        )
      }
    }
  }

}
