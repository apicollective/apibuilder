package lib.query

case class Query(parts: Seq[Part]) {

  val words: Seq[String] = {
    parts.flatMap { p =>
      p match {
        case Part.Text(value) => Some(value)
        case _ => None
      }
    }
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

  def apply(value: String): Seq[Part] = {
    assert(value == value.trim, "Value must be trimmed")
    value.split(":").toList match {
      case Nil => sys.error("Value cannot be empty")
      case one :: Nil => toText(one)
      case multiple => {
        multiple.head.toLowerCase match {
          case "org" => Seq(OrgKey(multiple.tail.mkString(":")))
          case other => {
            // Unrecognized key - just match on text
            toText(value)
          }
        }
      }
    }
  }

  private def toText(value: String): Seq[Part.Text] = {
    value.trim.split("\\s+").toSeq.map(_.trim).filter(!_.isEmpty).map(Part.Text(_))
  }

}

object QueryParser {

  def apply(q: String): Option[Query] = {
    q.trim.split("\\s+").flatMap( Part(_) ).toList match {
      case Nil => None
      case parts => Some(Query(parts))
    }
  }

}
