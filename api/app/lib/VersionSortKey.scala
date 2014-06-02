package lib

case class Version(version: String) extends Ordered[Version] {

  private val Dash = """\-"""
  private val Dot = """\."""
  private val Padding = 10000

  lazy val sortKey = {
    version.split(Dash).map { s =>
      val pieces = s.split(Dot)
      if (pieces.forall(s => isDigit(s))) {
        "5:%s".format(pieces.map( _.toInt + Padding ).mkString(":"))
      } else {
        "0:%s".format(s.toLowerCase)
      }
    }.mkString("|")
  }

  def compare(that: Version) = {
    sortKey.compare(that.sortKey)
  }

  private def isDigit(x: String) = {
    x.matches("^\\d+$")
  }

}

object VersionSortKey {

  def generate(v: String): String = {
    Version(v).sortKey
  }

}
