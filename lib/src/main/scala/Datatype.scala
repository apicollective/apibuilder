package lib

sealed trait Datatype {
  def types: Seq[Type]
  def label: String
}

object Datatype {

  private val Divider = " | "

  case class List(types: Seq[Type]) extends Datatype {
    override def label = types.map(_.name).mkString("[", Divider, "]")
  }

  case class Map(types: Seq[Type]) extends Datatype {
    override def label = types.map(_.name).mkString("map[", Divider, "]")
  }

  case class Option(types: Seq[Type]) extends Datatype {
    override def label = types.map(_.name).mkString("option[", Divider, "]")
  }

  case class Singleton(types: Seq[Type]) extends Datatype {
    override def label = types.map(_.name).mkString(Divider)
  }

}

case class Type(
  typeKind: TypeKind,
  name: String
)

sealed trait TypeKind

object TypeKind {

  case object Primitive extends TypeKind { override def toString = "primitive" }
  case object Model extends TypeKind { override def toString = "model" }
  case object Enum extends TypeKind { override def toString = "enum" }

}
