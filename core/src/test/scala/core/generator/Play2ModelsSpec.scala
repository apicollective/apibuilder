package core.generator

import java.io.File
import java.io.PrintWriter

import scala.sys.process._

import org.scalatest.FlatSpec

class Play2ModelsSpec extends FlatSpec {
  "Play2Models" should "generate scala" in {
    val json = scala.io.Source.fromFile("/web/svc-iris-hub/api.json").getLines.mkString("\n")
    val src = Play2Models(json)
    val dir = new File("/web/iris-hub-client/src/main/scala/irishub")
    dir.mkdirs
    val pw = new PrintWriter(new File(dir, "Client.scala"))
    try pw.println(src)
    pw.close()
    val token = sys.props.getOrElse("irishub.api.token", "")
    val url = sys.props.getOrElse("irishub.api.url", "")
    val proc = Process(
      Seq(
        "sbt",
        s"-Dirishub.api.token=${token}",
        s"-Dirishub.api.url=${url}",
        "compile"
      ),
      new File("/web/iris-hub-client")
    )
    assert(proc.! == 0)
  }
}
