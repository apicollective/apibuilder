package core.generator

import core.ServiceDescription
import org.scalatest.FlatSpec

class Play2RouteGeneratorSpec extends FlatSpec {

  it should "generate routes" in {
    val r = getClass.getResource("/api.json")
    if (r == null) {
      sys.error("Could not find resource named api.json")
    }
    val json = io.Source.fromURL(r).getLines.mkString("\n")
    val generator = Play2RouteGenerator(ServiceDescription(json))
    println(generator.generate())
  }

}
