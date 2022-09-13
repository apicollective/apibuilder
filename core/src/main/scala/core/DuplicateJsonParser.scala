package core

import io.circe.ParsingFailure
import io.circe.jawn.JawnParser


//  scala> parser.decode[Map[String, Int]]("""{"a":1,"a":2}""")

object DuplicateJsonParser {

  def validateDuplicates(value: String): Seq[String] = {
    val parser = new JawnParser(maxValueSize = None, allowDuplicateKeys = false)

    parser.parse(value) match {
      case Left(er: ParsingFailure) => Seq(er.message)
      case _ => Nil
    }
  }
}
