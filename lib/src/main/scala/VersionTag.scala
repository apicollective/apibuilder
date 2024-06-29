package lib

import lib.VersionTag.VersionTagType

object VersionTag {

  val Dash = """\-"""
  val Dot = """\."""
  val Separator = "|99999|"

  def isDigit(x: String): Boolean = {
    x.matches("^\\d+$")
  }

  sealed trait VersionTagType
  object VersionTagType {
    case class SemVer(tag: String, parts: List[Int]) extends VersionTagType {
      assert(parts.length >= 3, "SemVer tags need to have at least 3 parts")
    }
    case class Other(tag: String, parts: List[String]) extends VersionTagType {
      assert(parts.nonEmpty, "VersionTagType.Other: must have at least one tag")
    }
  }

  private[lib] def parse(value: String): VersionTagType = {
    value.split(VersionTag.Dot).toList match {
      case all if all.forall(isDigit) => {
        all.map(_.toInt) match {
          case one :: Nil => VersionTagType.SemVer(value, List(one, 0, 0))
          case one :: two :: Nil => VersionTagType.SemVer(value, List(one, two, 0))
          case tags => VersionTagType.SemVer(value, tags)
        }
      }
      case all => VersionTagType.Other(value, all)
    }
  }
}

case class VersionTag(version: String) extends Ordered[VersionTag] {
  private val trimmedVersion = version.trim

  private val Padding = 10000
  private val GithubVersionRx = """^v(\d+)$""".r
  private val parsed: List[VersionTagType] = trimmedVersion.split(VersionTag.Dash).map(VersionTag.parse).toList

  val sortKey: String = {
    parsed.map {
      case s: VersionTagType.SemVer => "5:%s".format(s.parts.map(_ + Padding).mkString(":"))
      case o: VersionTagType.Other => "0:%s".format(o.tag.toLowerCase)
    }.mkString("|") + "|9"
  }

  val major: Option[Int] = {
    parsed.headOption.flatMap {
      case s: VersionTagType.SemVer => s.parts.headOption
      case o: VersionTagType.Other => {
        o.parts.head match {
          case GithubVersionRx(number) => Some(number.toInt)
          case _ => None
        }
      }
    }
  }

  val qualifier: Option[String] = {
    trimmedVersion.split(VersionTag.Dash).toList match {
      case Nil => None
      case _ :: Nil => None
      case multiple => multiple.lastOption
    }
  }

  def compare(that: VersionTag): Int = {
    sortKey.compare(that.sortKey)
  }

  /**
   * Computes the next micro version. If we cannot parse the current
   * version number, then returns None.
   */
  def nextMicro(): Option[String] = {
    parsed match {
      case (tag: VersionTagType.SemVer) :: Nil => {
        Some((Seq(tag.parts.last + 1) ++ tag.parts.reverse.drop(1)).reverse.mkString("."))
      }
      case _ => None
    }
  }

}
