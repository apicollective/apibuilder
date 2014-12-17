package spec

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ApiJsonSpec extends FunSpec with Matchers {

  val Filenames = Seq("svc-iris-hub-0-0-1.json")
  val Dir = "core/src/test/resources"

  it("spec can parse itself") {
  }

}
