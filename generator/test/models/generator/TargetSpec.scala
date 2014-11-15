package generator

import models.TestHelper
import org.scalatest.{ FunSpec, Matchers }

class TargetSpec extends FunSpec with Matchers {

  private val Path = "api/api.json"
  private val service = TestHelper.parseFile(Path).serviceDescription.get

  private val code = service.models.find(_.name == "code").get
  lazy val target = code.fields.find(_.name == "target").getOrElse {
    sys.error("Cannot find code.target field")
  }

}
