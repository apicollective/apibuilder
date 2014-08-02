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
  }

  describe("generateEnumClass") {

    it("for enum with multiple values") {
      val values = Seq("Thirties", "Forties")
      val enums = RubyGemGenerator.generateEnumClass("User", "age_group", EnumerationFieldType(Datatype.StringType, values))
      enums.trim should be(TestHelper.readFile("core/src/test/resources/ruby-gem-enums.txt").trim)
    }

  }

  it("generate ruby") {
    val json = io.Source.fromFile(new File("reference-api/api.json")).getLines.mkString("\n")
    val generator = RubyGemGenerator(ServiceDescription(json))
    //println(generator.generate())
  }
}
