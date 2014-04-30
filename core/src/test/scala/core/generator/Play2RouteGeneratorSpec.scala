package core.generator

import core.ServiceDescription
import org.scalatest.FlatSpec

class Play2RouteGeneratorSpec extends FlatSpec {

  it should "generate routes" in {
    val json = io.Source.fromURL(getClass.getResource("/api.json")).getLines.mkString("\n")
    val generator = Play2RouteGenerator(ServiceDescription(json))
    println(generator.generate())
  }

}
