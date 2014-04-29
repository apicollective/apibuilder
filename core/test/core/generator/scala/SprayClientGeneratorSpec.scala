package core.generator.scala

import core.ServiceDescription
import org.scalatest.FlatSpec
import org.junit.Assert._

import java.io.File

import scala.sys.process._

class SprayClientGeneratorSpec extends FlatSpec {
  it should "generate scala" in {
    val json = scala.io.Source.fromFile("/web/svc-iris-hub/api.json").getLines.mkString("\n")
    println(SprayClientGenerator(ServiceDescription(json)))
    val token = sys.props.getOrElse("irishub.api.token", "")
    val url = sys.props.getOrElse("irishub.api.url", "")
    val proc = Process(
      Seq(
        "sbt",
        s"-Dirishub.api.token=${token}",
        s"-Dirishub.api.url=${url}",
        "run-main irishub.Tester"
      ),
      new File("/web/iris-hub-client")
    )
    assert(proc.! == 0)
  }
}
