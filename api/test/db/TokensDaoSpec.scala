package db

import org.scalatest.{FunSpec, Matchers}

class TokensDaoSpec extends FunSpec with Matchers with util.Daos {

  it("obfuscate") {
    tokensDao.obfuscate("") should be("XXX-XXXX-XXXX")
    tokensDao.obfuscate("123lkadfslkj34j123l4kabcde") should be("XXX-XXXX-bcde")
  }

}
