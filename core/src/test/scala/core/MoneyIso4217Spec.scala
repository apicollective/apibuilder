package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class MoneyIso4217Spec extends FunSpec with Matchers {

  it("parses valid values") {
    MoneyIso4217("USD 12.13") should be(MoneyIso4217(BigDecimal("12.13"), "USD"))
    MoneyIso4217("EUR 1") should be(MoneyIso4217(BigDecimal("1"), "EUR"))
  }

  it("accepts lower case currency codes") {
    MoneyIso4217("usd 12.13") should be(MoneyIso4217(BigDecimal("12.13"), "USD"))
  }

  it("validates invalid strings") {
    MoneyIso4217.validate("USD") should be(Seq("Invalid value for MoneyIso4217[USD] - expected something like 'USD 12.13'"))
    MoneyIso4217.validate("us 12") should be(Seq("requirement failed: Currency code[US] must be 3 characters"))
  }

}
