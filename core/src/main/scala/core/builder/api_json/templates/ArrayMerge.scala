package builder.api_json.templates

abstract class ArrayMerge[T]() {
  def label(i: T): String

  def merge(a: T, b: T): T

  final def merge(a: Seq[T], b: Seq[T]): Seq[T] = {
    val aByLabel = a.map { i => label(i) -> i }.toMap
    val bByLabel = b.map { i => label(i) -> i }.toMap
    b.map { bInstance =>
      aByLabel.get(label(bInstance)) match {
        case None => bInstance
        case Some(op) => merge(op, bInstance)
      }
    } ++ a.filterNot { op => bByLabel.contains(label(op)) }
  }
}
