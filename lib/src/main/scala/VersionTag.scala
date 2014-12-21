package lib

object VersionTag {

  val Dash = """\-"""
  val Dot = """\."""

  def isDigit(x: String) = {
    x.matches("^\\d+$")
  }

}

case class VersionTag(version: String) extends Ordered[VersionTag] {

  private val Padding = 10000

  lazy val sortKey = {
    version.split(VersionTag.Dash).map { s =>
      val pieces = s.split(VersionTag.Dot)
      if (pieces.forall(s => VersionTag.isDigit(s))) {
        "5:%s".format(pieces.map( _.toInt + Padding ).mkString(":"))
      } else {
        "0:%s".format(s.toLowerCase)
      }
    }.mkString("|")
  }

  def compare(that: VersionTag) = {
    sortKey.compare(that.sortKey)
  }

}


