package lib

sealed trait Datatype {
  def label: String
}

object Datatype {

  case class List(`type`: Type) extends Datatype {
    override def label = s"[${`type`.name}]"
  }

  case class Map(`type`: Type) extends Datatype {
    override def label = s"map[${`type`.name}]"
  }

  case class Option(`type`: Type) extends Datatype {
    override def label = s"option[${`type`.name}]"
  }

  case class Singleton(`type`: Type) extends Datatype {
    override def label = `type`.name
  }

  case class Union(types: Seq[Type]) extends Datatype {
    override def label = types.mkString("union[", ", ", "]")
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
