package builder.api_json.templates

abstract class MapMerge[T]() {

  def merge(a: T, b: T): T

  final def merge(a: Option[Map[String, T]], b: Option[Map[String, T]]): Option[Map[String, T]] = {
    OptionHelpers.flatten(a, b)(merge)
  }

  final def merge(a: Map[String, T], b: Map[String, T]): Map[String, T] = {
    a.filterNot { case (n, _) => b.contains(n) } ++ b.map { case (name, aInstance) =>
      a.get(name) match {
        case None => name -> aInstance
        case Some(bInstance) => name -> merge(aInstance, bInstance)
      }
    }
  }
}
