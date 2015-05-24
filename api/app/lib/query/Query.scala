package lib.query

case class Query(parts: Seq[Part]) {

  val text: String = {
    parts.flatMap { p =>
      p match {
        case Part.Text(value) => Some(value)
        case _ => None
      }
    }.mkString(" ")
  }

  val orgKeys: Seq[String] = {
    parts.flatMap { p =>
      p match {
        case Part.OrgKey(value) => Some(value)
        case _ => None
      }
    }
  }

}

sealed trait Part

object Part {

  case class Text(value: String) extends Part
  case class OrgKey(value: String) extends Part

  def apply(value: String): Part = {
    assert(value == value.trim, "Value must be trimmed")
    value.split(":").toList match {
      case Nil => sys.error("Value cannot be empty")
      case one :: Nil => Text(one)
      case multiple => {
        multiple.head match {
          case "org" => OrgKey(multiple.tail.mkString(":"))
          case other => {
            // Unrecognized key - just match on text
            Text(value)
          }
        }
      }
    }
  }

}

object QueryParser {

  def apply(q: String): Option[Query] = {
    q.trim.split("\\s+").map( Part(_) ).toList match {
      case Nil => None
      case parts => Some(Query(parts))
    }
  }

}
