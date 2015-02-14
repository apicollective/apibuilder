package lib

sealed trait TextDatatype {
  def typeName: String
  def label: String
}

object TextDatatype {

  private val Divider = " | "

  case class List(typeName: String) extends TextDatatype {
    override def label = s"[$typeName]"
  }

  case class Map(typeName: String) extends TextDatatype {
    override def label = s"map[$typeName]"
  }

  case class Singleton(typeName: String) extends TextDatatype {
    override def label = typeName
  }

  private val ListRx = "^\\[(.*)\\]$".r
  private val MapRx = "^map\\[(.*)\\]$".r
  private val MapDefaultRx = "^map$".r

  def apply(value: String): TextDatatype = {
    value match {
      case ListRx(t) => TextDatatype.List(t)
      case MapRx(t) => TextDatatype.Map(t)
      case MapDefaultRx() => TextDatatype.Map(Primitives.String.toString)
      case _ => TextDatatype.Singleton(value)
    }
  }

}
