package core.generator

import core.{ EnumerationFieldType, TestHelper }
import org.scalatest.{ FunSpec, Matchers }

class TargetSpec extends FunSpec with Matchers {

  private val Path = "api/api.json"
  private val service = TestHelper.parseFile(Path).serviceDescription.get

  private val code = service.models.find(_.name == "code").get
  lazy val target = code.fields.find(_.name == "target").getOrElse {
    sys.error("Cannot find code.target field")
  }

  it("api.json target lists all implemented keys in sort order") {
    val keys = target.fieldtype.asInstanceOf[EnumerationFieldType].values
    keys.sorted should be(keys)
  }

  it("api.json target lists all implemented targets") {
    val keys = target.fieldtype.asInstanceOf[EnumerationFieldType].values
    val implementedKeys = Target.Implemented.map(_.key).sorted
    keys should be(implementedKeys)
  }

  it("findByKey") {
    Target.findByKey("ADSFADSF") should be(None)
    Target.findByKey("ruby_client").get.key should be("ruby_client")
  }

  it("userAgent") {
    Target.userAgent(
      apidocVersion = "0.0.1",
      orgKey = "gilt",
      serviceKey = "apidoc",
      serviceVersion = "0.0.5"
    ) should be("www.apidoc.me:0.0.1 gilt/apidoc:0.0.5")
  }
}
