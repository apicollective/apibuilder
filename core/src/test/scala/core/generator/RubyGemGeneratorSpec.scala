package core.generator

import java.io.File
import core._

import core.ServiceDescription
import org.scalatest.{ FunSpec, Matchers }

class RubyGemGeneratorSpec extends FunSpec with Matchers {

  it("enumName") {
    RubyGemGenerator.enumName("CANCEL_REQUEST") should be("cancel_request")
    RubyGemGenerator.enumName("cancel_request") should be("cancel_request")
    RubyGemGenerator.enumName("cancelRequest") should be("cancel_request")
    RubyGemGenerator.enumName("CancelRequest") should be("cancel_request")
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
            
      val enums = RubyGemGenerator.generateEnumClass("age_group", EnumFieldType(enum))
      enums.trim should be(TestHelper.readFile("core/src/test/resources/ruby-gem-enums.txt").trim)
    }

  }

  it("generate ruby") {
    val json = io.Source.fromFile(new File("reference-api/api.json")).getLines.mkString("\n")
    val generator = RubyGemGenerator(ServiceDescription(json), "gilt 0.0.1-test")
    //println(generator.generate())
  }
}
