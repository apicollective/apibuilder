package models

import java.io.File
import com.gilt.apidocspec.models.{Enum, EnumValue}
import core._

import org.scalatest.{ FunSpec, Matchers }

class RubyClientGeneratorSpec extends FunSpec with Matchers {

  it("RubyUtil.toVariableName") {
    RubyUtil.toVariable("value", multiple = false) should be("value")
    RubyUtil.toVariable("value", multiple = true) should be("values")

    RubyUtil.toVariable("org_key", multiple = false) should be("org_key")
    RubyUtil.toVariable("org_key", multiple = true) should be("org_keys")

    RubyUtil.toVariable("orgKey", multiple = false) should be("org_key")
    RubyUtil.toVariable("orgKey", multiple = true) should be("org_keys")
  }

  it("RubyUtil.wrapInQuotes") {
    RubyUtil.wrapInQuotes("value") should be("'value'")
    RubyUtil.wrapInQuotes("'value'") should be(""""'value'"""")
  }

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
            
      TestHelper.assertEqualsFile("test/resources/ruby-gem-enums.txt", RubyClientGenerator.generateEnum(enum))
    }

  }

  it("generate ruby") {
    val json = io.Source.fromFile(new File("reference-api/api.json")).getLines.mkString("\n")
    val generator = RubyClientGenerator(ServiceBuilder(json), "gilt 0.0.1-test")
    TestHelper.assertEqualsFile("test/resources/ruby-client-generator-gilt-0.0.1-test.txt", generator.generate())
  }
}
