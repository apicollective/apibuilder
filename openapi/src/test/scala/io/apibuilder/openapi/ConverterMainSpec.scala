package io.apibuilder.openapi

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ConverterMainSpec extends AnyWordSpec with Matchers {

  "parseArgs" must {

    "parse valid --file and --organization flags" in {
      val result = ConverterMain.parseArgs(List("foo.json", "--organization", "my-org"))
      result must be(Symbol("right"))
      val opts = result.toOption.get
      opts.input must be(Some("foo.json"))
      opts.organization must be(Some("my-org"))
    }

    "return Left when --organization is missing" in {
      val result = ConverterMain.parseArgs(List("foo.json"))
      result must be(Symbol("left"))
      result.left.get must include("--organization")
    }

    "return Left when no input is provided" in {
      val result = ConverterMain.parseArgs(List("--organization", "my-org"))
      result must be(Symbol("left"))
      result.left.get must include("input")
    }

    "return Left for an unknown flag" in {
      val result = ConverterMain.parseArgs(List("foo.json", "--organization", "my-org", "--unknown-flag"))
      result must be(Symbol("left"))
      result.left.get must include("--unknown-flag")
    }

    "accumulate multiple --filter-header values" in {
      val result = ConverterMain.parseArgs(
        List(
          "foo.json",
          "--organization",
          "my-org",
          "--filter-header",
          "X-Api-Key",
          "--filter-header",
          "X-Trace-Id",
        ),
      )
      result must be(Symbol("right"))
      val opts = result.toOption.get
      opts.filterHeaders must be(Set("X-Api-Key", "X-Trace-Id"))
    }
  }
}
