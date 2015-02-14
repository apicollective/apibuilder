package lib

import org.scalatest.{FunSpec, Matchers}

class LabelHelperSpec extends FunSpec with Matchers {

  it("token") {
    LabelHelper.token("") should be("XXXX-XXXX-XXXX")
    LabelHelper.token("123lkadfslkj34j123l4kabcde") should be("123-XXXX-bcde")
  }

}
