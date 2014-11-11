package models

import core._
import core.generator.ScalaServiceDescription
import org.scalatest.{ ShouldMatchers, FunSpec }

class Play2JsonSpec extends FunSpec with ShouldMatchers {

  describe("quality schema") {

    lazy val quality = new ScalaServiceDescription(TestHelper.parseFile("test/resources/examples/quality.json").serviceDescription.get)
    lazy val play2Json = Play2Json(quality.name)

    describe("plan") {

      lazy val plan = quality.models.find(_.name == "Plan").get

      it("readers") {
        TestHelper.assertEqualsFile(
          "test/resources/generators/play-2-json-spec-quality-plan-readers.txt",
          play2Json.readers(plan)
        )
      }

      it("writers") {
        TestHelper.assertEqualsFile(
          "test/resources/generators/play-2-json-spec-quality-plan-writers.txt",
          play2Json.writers(plan)
        )
      }
    }

    describe("healthcheck") {

      lazy val healthcheck = quality.models.find(_.name == "Healthcheck").get

      it("readers") {
        TestHelper.assertEqualsFile(
          "test/resources/generators/play-2-json-spec-quality-healthcheck-readers.txt",
          play2Json.readers(healthcheck)
        )
      }

      it("writers") {
        TestHelper.assertEqualsFile(
          "test/resources/generators/play-2-json-spec-quality-healthcheck-writers.txt",
          play2Json.writers(healthcheck)
        )
      }
    }
  }

}
