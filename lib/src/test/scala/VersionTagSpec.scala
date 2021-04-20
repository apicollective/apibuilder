package lib

import lib.VersionTag.VersionTagType
import org.scalatest.{Assertion, FunSpec, Matchers}

class VersionTagSpec extends FunSpec with Matchers {

  private def assertSorted(versions: Seq[String], target: String): Assertion = {
    val versionObjects = versions.map( VersionTag(_) )
    versionObjects.sorted.map(_.version).mkString(" ") should be(target)
  }

  it("sorts developer tags before release tags (latest release tag should be last)") {
    assertSorted(Seq("1.0.0", "1.0.0-g-1"), "1.0.0-g-1 1.0.0")
    assertSorted(Seq("0.6.0-3-g3b52fba", "0.7.6"), "0.6.0-3-g3b52fba 0.7.6")

    assertSorted(Seq("0.28.1", "0.28.1-dev"), "0.28.1-dev 0.28.1")
    assertSorted(Seq("0.28.1-dev", "0.28.1"), "0.28.1-dev 0.28.1")
  }

  it("sorts 1 element version") {
    assertSorted(Seq("0", "1", "5"), "0 1 5")
    assertSorted(Seq("5", "0", "1"), "0 1 5")
    assertSorted(Seq("2", "1", "0"), "0 1 2")
  }

  it("sorts 2 element version") {
    assertSorted(Seq("0.0", "0.1", "2.1"), "0.0 0.1 2.1")
    assertSorted(Seq("0.0", "0.1", "2.1"), "0.0 0.1 2.1")
    assertSorted(Seq("1.0", "0.0", "1.1", "1.2", "0.10"), "0.0 0.10 1.0 1.1 1.2")
  }

  it("sorts 3 element version") {
    assertSorted(Seq("0.0.0", "0.0.1", "0.1.0", "5.1.0"), "0.0.0 0.0.1 0.1.0 5.1.0")
    assertSorted(Seq("10.10.10", "10.0.1", "1.1.50", "15.2.2", "1.0.10"), "1.0.10 1.1.50 10.0.1 10.10.10 15.2.2")
  }

  it("numeric tags are considered newer than string tags") {
    assertSorted(Seq("1.0.0", "r20140201.1"), "r20140201.1 1.0.0")
  }

  it("sorts string tags as strings") {
    assertSorted(Seq("r20140201.1", "r20140201.2"), "r20140201.1 r20140201.2")
  }

  it("sorts strings mixed with semver tags") {
    assertSorted(Seq("0.8.6", "0.8.8", "development"), "development 0.8.6 0.8.8")
  }

  it("parses major from semver versions") {
    VersionTag("0.0.0").major should be(Some(0))
    VersionTag("0.0.0").major should be(Some(0))
    VersionTag("0.0.0-dev").major should be(Some(0))

    VersionTag("1.0.0").major should be(Some(1))
    VersionTag("1.0.0-dev").major should be(Some(1))
  }

  it("parses major from github versions") {
    VersionTag("v1").major should be(Some(1))
    VersionTag("v1.0.0").major should be(Some(1))
    VersionTag("v1.0.0-dev").major should be(Some(1))
  }

  it("returns none when no major number") {
    VersionTag("v").major should be(None)
    VersionTag("dev").major should be(None)
  }

  it("major ignores whitespace") {
    VersionTag(" 1.0").major should be(Some(1))
    VersionTag(" v2.0").major should be(Some(2))
  }

  it("nextMicro") {
    VersionTag("foo").nextMicro() should be(None)
    VersionTag("0.0.1").nextMicro() should be(Some("0.0.2"))
    VersionTag("1.2.3").nextMicro() should be(Some("1.2.4"))
    VersionTag("0.0.5-dev").nextMicro() should be(None)
  }

  it("qualifier") {
    VersionTag("foo").qualifier should be(None)
    VersionTag("0.0.1").qualifier should be(None)
    VersionTag("0.0.5-dev").qualifier should be(Some("dev"))
  }

  it("sorts versions w/ varying lengths") {
    assertSorted(Seq("1", "0.1"), "0.1 1")
    assertSorted(Seq("1", "0.1", "0.0.1"), "0.0.1 0.1 1")
    assertSorted(Seq("1.2", "1.2.1"), "1.2 1.2.1")
    assertSorted(Seq("1.2", "1.2.1", "2"), "1.2 1.2.1 2")
  }

  it("parse") {
    VersionTag.parse("") should be(
      VersionTagType.Other("", List(""))
    )
    VersionTag.parse("1-dev") should be(
      VersionTagType.Other("1-dev", List("1-dev"))
    )

    VersionTag.parse("1") should be(
      VersionTagType.SemVer("1", List(1, 0, 0))
    )
    VersionTag.parse("1.2") should be(
      VersionTagType.SemVer("1.2", List(1, 2, 0))
    )
    VersionTag.parse("1.2.3") should be(
      VersionTagType.SemVer("1.2.3", List(1, 2, 3))
    )
    VersionTag.parse("1.2.3.4") should be(
      VersionTagType.SemVer("1.2.3.4", List(1, 2, 3, 4))
    )
  }
}
