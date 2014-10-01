package core.generator

import java.io.File
import codegenerator.models.{Enum, EnumValue}
import core._

import org.scalatest.{ FunSpec, Matchers }

class RubyClientGeneratorSpec extends FunSpec with Matchers {

  it("enumName") {
    RubyClientGenerator.enumName("CANCEL_REQUEST") should be("cancel_request")
    RubyClientGenerator.enumName("cancel_request") should be("cancel_request")
    RubyClientGenerator.enumName("cancelRequest") should be("cancel_request")
    RubyClientGenerator.enumName("CancelRequest") should be("cancel_request")
  }

  describe("generateEnumClass") {

    it("for enum with multiple values") {
      val enum = Enum(
        name = "age_group",
        description = None,
        values = Seq(
          EnumValue(
            name = "Thirties",
            description = None
          ),
          EnumValue(
            name = "Forties",
            description = None
          )
        )
      )
            
      val enums = RubyClientGenerator.generateEnum(enum)
      enums.trim should be(TestHelper.readFile("core/src/test/resources/ruby-gem-enums.txt").trim)
    }

  }

  it("generate ruby") {
    val json = io.Source.fromFile(new File("reference-api/api.json")).getLines.mkString("\n")
    val generator = RubyClientGenerator(ServiceDescriptionBuilder(json), "gilt 0.0.1-test")
    //println(generator.generate())
  }
}
