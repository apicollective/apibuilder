package core.generator

import core.TestHelper
import org.scalatest.{ FunSpec, Matchers }

class TargetSpec extends FunSpec with Matchers {

  private val Path = "api/api.json"
  private val service = TestHelper.parseFile(Path).serviceDescription.get

  private val code = service.models.find(_.name == "code").get
  lazy val target = code.fields.find(_.name == "target").getOrElse {
    sys.error("Cannot find code.target field")
  }

  it("CodeGenTarget target lists all implemented keys in sort order") {
    val keys = CodeGenTarget.Implemented.map(_.key)
    keys.sorted should be(keys)
  }

  it("findByKey") {
    CodeGenTarget.findByKey("ADSFADSF") should be(None)
    CodeGenTarget.findByKey("ruby_client").get.key should be("ruby_client")
  }
}
