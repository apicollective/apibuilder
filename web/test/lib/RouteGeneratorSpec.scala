package lib

import org.scalatest.FlatSpec
import org.junit.Assert._

class RouteGeneratorSpec extends FlatSpec {

  it should "generate routes" in {
    val generator = RouteGenerator.fromFile("./api.json")
    println(generator.generate())
  }

}
