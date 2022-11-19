package builder.api_json.templates

abstract class ArrayMerge[T]() {
  def uniqueIdentifier(i: T): String

  def merge(a: T, b: T): T

  final def merge(a: Option[Seq[T]], b: Option[Seq[T]]): Option[Seq[T]] = {
    OptionHelpers.flatten(a, b)(merge)
  }

  final def merge(a: Seq[T], b: Seq[T]): Seq[T] = {
    val aByLabel = a.map { i => uniqueIdentifier(i) -> i }.toMap
    val bByLabel = b.map { i => uniqueIdentifier(i) -> i }.toMap
    val all = a.map(uniqueIdentifier) ++ b.map(uniqueIdentifier).filterNot(aByLabel.contains)
    all.flatMap { identifier =>
      OptionHelpers.flatten(aByLabel.get(identifier), bByLabel.get(identifier))(merge)
    }
  }
}
