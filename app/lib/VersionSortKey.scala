package lib

case class Version(version: String) extends Ordered[Version] {

  private val Dot = """\."""
  private val Padding = 10000

  lazy val sortKey = {
    val pieces = version.split(Dot)
    if (pieces.forall(s => isDigit(s))) {
      "a:%s".format(pieces.map( _.toInt + Padding ).mkString(":"))
    } else {
      s"z:$version".toLowerCase
    }
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
