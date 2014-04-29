package core.generator.scala

import core.ServiceDescription
import org.scalatest.FlatSpec
import org.junit.Assert._

import scala.sys.process._

class SprayClientGeneratorSpec extends FlatSpec {
  it should "generate scala" in {
    val json = scala.io.Source.fromFile("/web/svc-iris-hub/api.json").getLines.mkString("\n")
    println(SprayClientGenerator(ServiceDescription(json)))
    val status: Int = ("cd /web/api-doc-client && sbt compile" !)
    assert(status == 0)
  }
}
