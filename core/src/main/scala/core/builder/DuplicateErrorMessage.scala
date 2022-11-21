package builder

import lib.Text

import cats.implicits._
import cats.data.ValidatedNec

private[builder] object DuplicateErrorMessage {
  def validate(label: String, values: Iterable[String]): ValidatedNec[String, Unit] = {
    findDuplicates(values) match {
      case Nil => ().validNec
      case dups => dups.map { n =>
        s"$label[$n] appears more than once".invalidNec
      }.sequence.map(_ => ())
    }
  }

  private[this] def findDuplicates(values: Iterable[String]): List[String] = {
    values.groupBy(Text.camelCaseToUnderscore(_).toLowerCase.trim)
      .filter {
        _._2.size > 1
      }
      .keys.toList.sorted
  }
}