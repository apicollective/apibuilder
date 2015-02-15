package db

import org.scalatest.{FunSpec, Matchers}

class TokensDaoSpec extends FunSpec with Matchers {

  it("obfuscate") {
    TokensDao.obfuscate("") should be("XXXX-XXXX-XXXX")
    TokensDao.obfuscate("123lkadfslkj34j123l4kabcde") should be("123-XXXX-bcde")
  }

}
