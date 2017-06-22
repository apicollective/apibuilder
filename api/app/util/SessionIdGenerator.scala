package util

object SessionIdGenerator {

  private[this] val Prefix = "A51"
  private[this] val RandomLength = 64 - Prefix.length
  private[this] val random = new Random()

  def generate(): String = {
    "%s%s".format(Prefix, random.alphaNumeric(RandomLength))
  }

}
