package core.generator

import core.ServiceDescription
import org.scalatest.FlatSpec
import org.junit.Assert._

class RubyGemGeneratorSpec extends FlatSpec {

  it should "generate ruby" in {
    val json = scala.io.Source.fromFile("../svc/api.json").getLines.mkString("\n")
    val generator = RubyGemGenerator(ServiceDescription(json))
    println(generator.generate())
  }

}
