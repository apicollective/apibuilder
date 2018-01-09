package lib

object VersionTag {

  val Dash = """\-"""
  val Dot = """\."""
  val Separator = "|99999|"

  def isDigit(x: String) = {
    x.matches("^\\d+$")
  }

}

case class VersionTag(version: String) extends Ordered[VersionTag] {
  private[this] val trimmedVersion = version.trim

  private[this] val Padding = 10000
  private[this] val GithubVersionRx = """^v(\d+)$""".r

  val sortKey: String = {
    trimmedVersion.split(VersionTag.Dash).map { s =>
      val pieces = splitOnDot(s)
      if (pieces.forall(s => VersionTag.isDigit(s))) {
        "5:%s".format(pieces.map( _.toInt + Padding ).mkString(":"))
      } else {
        "0:%s".format(s.toLowerCase)
      }
    }.mkString("|") + "|9"
  }

  val major: Option[Int] = {
    trimmedVersion.split(VersionTag.Dash).headOption.flatMap { s =>
      splitOnDot(s).headOption.flatMap { value =>
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

  val qualifier: Option[String] = {
    trimmedVersion.split(VersionTag.Dash).toList match {
      case Nil => None
      case one :: Nil => None
      case multiple => multiple.lastOption
    }
  }

  def compare(that: VersionTag) = {
    sortKey.compare(that.sortKey)
  }

  /**
   * Computes the next micro version. If we cannot parse the current
   * version number, then returns None.
   */
  def nextMicro(): Option[String] = {
    trimmedVersion.split(VersionTag.Dash).size match {
      case 1 => {
        val pieces = splitOnDot(version)
        if (pieces.forall(s => VersionTag.isDigit(s))) {
          Some((Seq(pieces.last.toInt + 1) ++ pieces.reverse.drop(1)).reverse.mkString("."))
        } else {
          None
        }
      }
      case _ => None
    }
  }

  /**
   * Splits on dot and as long as the numbers are numeric, ensures
   * that the returned array has at least 3 elements. So
   * splitOnDot("1") would return Seq("1", "0", "0").
   */
  private[this] def splitOnDot(value: String): Seq[String] = {
    var pieces = value.split(VersionTag.Dot)
    if (pieces.forall(s => VersionTag.isDigit(s))) {
      while (pieces.length < 3) {
        pieces = pieces ++ Seq("0")
      }
    }
    pieces
  }

}
