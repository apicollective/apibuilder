package lib

sealed trait TextDatatype

object TextDatatype {

  case object List extends TextDatatype
  case object Map extends TextDatatype
  case class Singleton(name: String) extends TextDatatype

  private[this] val ListRx = "^\\[(.*)\\]$".r
  private[this] val MapRx = "^map\\[(.*)\\]$".r
  private[this] val MapDefaultRx = "^map$".r

  def parse(value: String): Seq[TextDatatype] = {
    value match {
      case ListRx(t) => Seq(TextDatatype.List) ++ parse(t)
      case MapRx(t) => Seq(TextDatatype.Map) ++ parse(t)
      case MapDefaultRx() => Seq(TextDatatype.Map, TextDatatype.Singleton(Primitives.String.toString))
      case _ => Seq(TextDatatype.Singleton(value))
    }
  }

  def label(types: Seq[TextDatatype]): String = {
    types.toList match {
      case Nil => ""
      case one :: rest => {
        one match {
          case List => "[" + label(rest) + "]"
          case Map => "map[" + label(rest) + "]"
          case Singleton(n) => n + label(rest)
        }
      }
    }
  }
}
