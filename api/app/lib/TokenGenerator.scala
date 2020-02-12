package lib

import java.security.SecureRandom

import scala.util.Random

object TokenGenerator {

  private[this] val random = new Random(new SecureRandom())
  private[this] val Alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

  def generate(n: Int = 80): String = {
    LazyList.continually(random.nextInt(Alphabet.size)).map(Alphabet).take(n).mkString
  }

}
