package lib

import java.security.SecureRandom
import java.util.UUID
import scala.util.Random

object TokenGenerator {

  private[this] val random = new Random(new SecureRandom())
  private[this] val Alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

  def generate(n: Int = 80): String = {
    val uuid = UUID.randomUUID().toString.replaceAll("-", "")
    val numberRandom = n - uuid.length
    if (numberRandom > 0) {
      uuid + random(numberRandom)
    } else {
      random(n)
    }
  }

  private[this] def random(n: Int): String = {
    LazyList.continually(random.nextInt(Alphabet.length)).map(Alphabet).take(n).mkString
  }
}
