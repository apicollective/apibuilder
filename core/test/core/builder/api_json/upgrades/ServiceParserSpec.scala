package builder.api_json.upgrades

import helpers.{ServiceHelpers, ValidatedTestHelpers}
import io.apibuilder.spec.v0.models.json._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

import scala.annotation.nowarn

@nowarn("msg=value apidoc in class Service is deprecated: This field is no longer used in API Builder and may be removed in the future.")
class ServiceParserSpec extends AnyFunSpec with Matchers with ServiceHelpers with ValidatedTestHelpers {

  private def parser: ServiceParser = ServiceParser()

  it("fromString") {
    expectInvalid {
      parser.fromString("")
    }.head.contains("Invalid JSON") shouldBe true

    expectInvalid {
      parser.fromString("{}")
    }

    expectValid {
      parser.fromString(Json.toJson(makeService()).toString)
    }
  }

  it("fromJson") {
    expectInvalid {
      parser.fromJson(Json.obj())
    }

    expectValid {
      parser.fromJson(Json.toJson(makeService()))
    }
  }

  it("injects 'apidoc' node if missing") {
    val s = makeService()
    s.apidoc shouldBe None
    expectValid {
      parser.fromJson(Json.toJson(makeService()))
    }.apidoc.getOrElse {
      sys.error("Missing apidoc node")
    }
  }

}