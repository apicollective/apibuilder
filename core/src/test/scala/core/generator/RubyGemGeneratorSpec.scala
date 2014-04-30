package core.generator

import core.ServiceDescription
import org.scalatest.FlatSpec

class RubyGemGeneratorSpec extends FlatSpec {

  it should "generate ruby" in {
    val json = io.Source.fromURL(getClass.getResource("/api.json")).getLines.mkString("\n")
    val generator = RubyGemGenerator(ServiceDescription(json))
    println(generator.generate())
  }

}
