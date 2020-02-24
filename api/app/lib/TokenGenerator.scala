package lib

import scala.util.Random
import java.security.SecureRandom

object TokenGenerator {

  private[this] val random = new Random(new SecureRandom())
  private[this] val Alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

  def generate(n: Int = 80): String = {
    Stream.continually(random.nextInt(Alphabet.size)).map(Alphabet).take(n).mkString
  }

}
