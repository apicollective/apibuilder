package builder.api_json.templates

private[templates] object OptionHelpers {

  def flatten[T](a: Option[T], b: Option[T])(
    f: (T, T) => T
  ): Option[T] = {
    (a, b) match {
      case (None, None) => None
      case (Some(a), None) => Some(a)
      case (None, Some(b)) => Some(b)
      case (Some(a), Some(b)) => Some(f(a, b))
    }
  }

}
