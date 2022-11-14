package builder

import lib.Text

private[builder] object DuplicateErrorMessage {
  def message(label: String, values: Iterable[String]): Seq[String] = {
    findDuplicates(values).map { n =>
      s"$label[$n] appears more than once"
    }
  }

  def findDuplicates(values: Iterable[String]): List[String] = {
    values.groupBy(Text.camelCaseToUnderscore(_).toLowerCase.trim)
      .filter {
        _._2.size > 1
      }
      .keys.toList.sorted
  }
}