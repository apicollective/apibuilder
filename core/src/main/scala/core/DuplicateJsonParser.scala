package core

import cats.implicits._
import cats.data.ValidatedNec
import io.circe.ParsingFailure
import io.circe.jawn.JawnParser


//  scala> parser.decode[Map[String, Int]]("""{"a":1,"a":2}""")

object DuplicateJsonParser {

  def validateDuplicates(value: String): ValidatedNec[String, Unit] = {
    val parser = new JawnParser(maxValueSize = None, allowDuplicateKeys = false)

    parser.parse(value) match {
      case Left(er: ParsingFailure) => er.message.invalidNec
      case _ => ().validNec
    }
  }
}
