package core

case class MoneyIso4217(value: BigDecimal, currencyCode: String) {

  require(currencyCode.toUpperCase == currencyCode, s"Currency code[${currencyCode}] must be upper case")
  require(currencyCode.length == 3, s"Currency code[${currencyCode}] must be 3 characters")

}

object MoneyIso4217 {

  def apply(s: String): MoneyIso4217 = {
    val parts = s.split(" ")
    if (parts.length != 2) {
      throw new IllegalArgumentException(s"Invalid value for MoneyIso4217[$s] - expected something like 'USD 12.13'")
    }
    MoneyIso4217(BigDecimal(parts(1)), parts(0).toUpperCase)
  }

  def validate(value: String): Seq[String] = {
    try {
      MoneyIso4217(value)
      Seq.empty
    } catch {
      case e: IllegalArgumentException => {
        Seq(e.getMessage)
      }
      case e: Throwable => {
        Seq(e.getMessage)
      }
    }
  }

}
