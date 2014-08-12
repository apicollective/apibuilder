package core.generator

import core.{ EnumFieldType, TestHelper }
import org.scalatest.{ FunSpec, Matchers }

class TargetSpec extends FunSpec with Matchers {

  private val Path = "api/api.json"
  private val service = TestHelper.parseFile(Path).serviceDescription.get

  private val code = service.models.find(_.name == "code").get
  lazy val target = code.fields.find(_.name == "target").getOrElse {
    sys.error("Cannot find code.target field")
  }

  it("api.json target lists all implemented keys in sort order") {
    val keys = target.fieldtype.asInstanceOf[EnumFieldType].enum.values.map(_.name)
    keys.sorted should be(keys)
  }

  it("api.json target lists all implemented targets") {
    val keys = target.fieldtype.asInstanceOf[EnumFieldType].enum.values.map(_.name)
    val implementedKeys = Target.Implemented.map(_.key).sorted
    keys should be(implementedKeys)
  }

  it("findByKey") {
    Target.findByKey("ADSFADSF") should be(None)
    Target.findByKey("ruby_client").get.key should be("ruby_client")
  }

  it("userAgent") {
    Target.findByKey("ruby_client").get.userAgent(
      apidocVersion = "0.0.1",
      orgKey = "gilt",
      serviceKey = "user",
      serviceVersion = "1.0.5"
    ) should be("apidoc:0.0.1 http://www.apidoc.me/gilt/code/user/1.0.5/ruby_client")
  }
}
