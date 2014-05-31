package core.generator

import java.io.File
import java.io.PrintWriter

import scala.sys.process._

import org.scalatest.FlatSpec

class Play2ModelsSpec extends FlatSpec {
  "Play2Models" should "generate scala" in {
    val json = io.Source.fromURL(getClass.getResource("/api.json")).getLines.mkString("\n")
    println(Play2Models(json))
  }
}
