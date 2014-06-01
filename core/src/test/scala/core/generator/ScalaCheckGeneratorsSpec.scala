package core.generator

import java.io.File
import java.io.PrintWriter

import scala.sys.process._

import core._

import org.scalatest.FlatSpec

class ScalaCheckGeneratorsSpec extends FlatSpec {
  "ScalaCheckGenerators" should "generate scala" in {
    val json = io.Source.fromURL(getClass.getResource("/api.json")).getLines.mkString("\n")
    val code = ScalaCheckGenerators(json)
    println(code)
  }
}
