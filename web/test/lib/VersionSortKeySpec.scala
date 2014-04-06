package lib

import org.scalatest.FlatSpec
import org.junit.Assert._

class VersionSortKeySpec extends FlatSpec {

  it should "pad 1 element version" in {
    assertEquals("a:10000", VersionSortKey.generate("0"))
    assertEquals("a:10001", VersionSortKey.generate("1"))
    assertEquals("a:10005", VersionSortKey.generate("5"))
  }

  it should "sort 1 element version" in {
    val versions = Seq(Version("1"), Version("0"), Version("2"))
    assertEquals("0 1 2",
                 versions.sorted.map(_.version).mkString(" "))
  }

  it should "pad 2 element versions" in {
    assertEquals("a:10000:10000", VersionSortKey.generate("0.0"))
    assertEquals("a:10000:10001", VersionSortKey.generate("0.1"))
    assertEquals("a:10002:10001", VersionSortKey.generate("2.1"))
  }

  it should "sort 2 element version" in {
    val versions = Seq(Version("1.0"), Version("0.0"), Version("1.1"), Version("1.2"), Version("0.10"))
    assertEquals("0.0 0.10 1.0 1.1 1.2",
                 versions.sorted.map(_.version).mkString(" "))
  }

  it should "pad 3 element versions" in {
    assertEquals("a:10000:10000:10000", VersionSortKey.generate("0.0.0"))
    assertEquals("a:10000:10000:10001", VersionSortKey.generate("0.0.1"))
    assertEquals("a:10000:10001:10000", VersionSortKey.generate("0.1.0"))
    assertEquals("a:10005:10001:10000", VersionSortKey.generate("5.1.0"))
  }

  it should "sort 3 element version" in {
    val versions = Seq(Version("10.10.10"), Version("10.0.1"), Version("1.1.50"), Version("15.2.2"), Version("1.0.10"))
    assertEquals("1.0.10 1.1.50 10.0.1 10.10.10 15.2.2",
                 versions.sorted.map(_.version).mkString(" "))
  }

}
