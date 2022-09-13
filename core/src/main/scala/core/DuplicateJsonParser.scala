package core

import io.circe.jawn.JawnParser

//  scala> parser.decode[Map[String, Int]]("""{"a":1,"a":2}""")

object DuplicateJsonParser {

  def foo(value: String): Seq[String] = {
    val parser = JawnParser(allowDuplicateKeys = false)
    parser.decode(value) match {
      case Left(er) => {
        println(s"ERR: $er")
        Nil
      }
      case Right(js) => {
        println(s"JS: $js")
        Nil
      }
    }
  }
}
