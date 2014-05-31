package core.generator

import java.io.File

import core.ServiceDescription
import org.scalatest.FlatSpec

class RubyGemGeneratorSpec extends FlatSpec {

  it should "generate ruby" in {
    val json = io.Source.fromFile(new File("reference-api/api.json")).getLines.mkString("\n")
    val generator = RubyGemGenerator(ServiceDescription(json))
    println(generator.generate())
  }

}
