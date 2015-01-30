package lib

object VersionTag {

  val Dash = """\-"""
  val Dot = """\."""

  def isDigit(x: String) = {
    x.matches("^\\d+$")
  }

}

case class VersionTag(version: String) extends Ordered[VersionTag] {
  private val trimmedVersion = version.trim

  private val Padding = 10000
  private val GithubVersionRx = """^v(\d+)$""".r

  val sortKey: String = {
    trimmedVersion.split(VersionTag.Dash).map { s =>
      val pieces = s.split(VersionTag.Dot)
      if (pieces.forall(s => VersionTag.isDigit(s))) {
        "5:%s".format(pieces.map( _.toInt + Padding ).mkString(":"))
      } else {
        "0:%s".format(s.toLowerCase)
      }
    }.mkString("|")
  }

  val major: Option[Int] = {
    trimmedVersion.split(VersionTag.Dash).headOption.flatMap { s =>
      s.split(VersionTag.Dot).headOption.flatMap { value =>
        VersionTag.isDigit(value) match {
          case true => Some(value.toInt)
          case false => value match {
            case GithubVersionRx(number) => Some(number.toInt)
            case _ => None
          }
        }
      }
    }
  }

  def compare(that: VersionTag) = {
    sortKey.compare(that.sortKey)
  }

  /**
   * Computes the next micro version. If we cannot parse the currency
   * version number, then returns None.
   */
  def nextMicro(): Option[String] = {
    trimmedVersion.split(VersionTag.Dash).size match {
      case 1 => {
        val pieces = version.split(VersionTag.Dot)
        if (pieces.forall(s => VersionTag.isDigit(s))) {
          Some((Seq(pieces.last.toInt + 1) ++ pieces.reverse.drop(1)).reverse.mkString("."))
        } else {
          None
        }
      }
      case _ => None
    }
  }

}


