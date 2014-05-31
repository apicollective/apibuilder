package core.generator

import java.io.File
import java.io.PrintWriter

import scala.sys.process._

import core._

import org.scalatest.FlatSpec

class Play2ClientGeneratorSpec extends FlatSpec {
  "Play2ClientGenerator" should "generate scala" in {
    val json = io.Source.fromURL(getClass.getResource("/api.json")).getLines.mkString("\n")
    println(Play2ClientGenerator(json))
  }
}
