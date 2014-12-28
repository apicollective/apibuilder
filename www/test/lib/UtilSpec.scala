package lib

import com.gilt.apidocgenerator.models.Container
import org.scalatest.{ ShouldMatchers, FunSpec }

class UtilSpec extends FunSpec with ShouldMatchers {

  it("calculateNextVersion") {
    Util.calculateNextVersion("foo") should be("foo")
    Util.calculateNextVersion("0.0.1") should be("0.0.2")
    Util.calculateNextVersion("1.2.3") should be("1.2.4")
    Util.calculateNextVersion("0.0.5-dev") should be("0.0.5-dev")
  }
}
