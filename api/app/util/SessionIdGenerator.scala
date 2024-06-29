package util

object SessionIdGenerator {

  private val Prefix = "A51"
  private val RandomLength = 64 - Prefix.length
  private val random = new Random()

  def generate(): String = {
    "%s%s".format(Prefix, random.alphaNumeric(RandomLength))
  }

}
