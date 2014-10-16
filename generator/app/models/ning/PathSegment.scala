package models.ning

/**
  * This file has been modified from
  * https://github.com/playframework/playframework/blob/2.3.x/framework/src/play/src/main/scala/play/utils/UriEncoding.scala
  * 
  * This file contains software that is licensed under the Apache 2 license.
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with
  * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
  */
private[ning] object PathSegment {

  val definition = """
object PathSegment {
  // See https://github.com/playframework/playframework/blob/2.3.x/framework/src/play/src/main/scala/play/utils/UriEncoding.scala
  def encode(s: String, inputCharset: String): String = {
    val in = s.getBytes(inputCharset)
    val out = new java.io.ByteArrayOutputStream()
    for (b <- in) {
      val allowed = segmentChars.get(b & 0xFF)
      if (allowed) {
        out.write(b)
      } else {
        out.write('%')
        out.write(upperHex((b >> 4) & 0xF))
        out.write(upperHex(b & 0xF))
      }
    }
    out.toString("US-ASCII")
  }

  private def upperHex(x: Int): Int = {
    // Assume 0 <= x < 16
    if (x < 10) (x + '0') else (x - 10 + 'A')
  }

  private val segmentChars: java.util.BitSet = membershipTable(pchar)

  private def pchar: Seq[Char] = {
    val alphaDigit = for ((min, max) <- Seq(('a', 'z'), ('A', 'Z'), ('0', '9')); c <- min to max) yield c
    val unreserved = alphaDigit ++ Seq('-', '.', '_', '~')
    val subDelims = Seq('!', '$', '&', '\'', '(', ')', '*', '+', ',', ';', '=')
    unreserved ++ subDelims ++ Seq(':', '@')
  }

  private def membershipTable(chars: Seq[Char]): java.util.BitSet = {
    val bits = new java.util.BitSet(256)
    for (c <- chars) { bits.set(c.toInt) }
    bits
  }
}
""".trim

}
