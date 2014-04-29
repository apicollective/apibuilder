package core.generator

import core.ServiceDescription
import org.scalatest.FlatSpec
import org.junit.Assert._

class Play2RouteGeneratorSpec extends FlatSpec {

  it should "generate routes" in {
    val json = scala.io.Source.fromFile("../svc/api.json").getLines.mkString("\n")
    val generator = Play2RouteGenerator(ServiceDescription(json))
    println(generator.generate())
  }

}
