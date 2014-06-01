package core.generator

import java.io.File
import java.io.PrintWriter

import scala.sys.process._

import core._

import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers

class ReferenceApiSpec extends FlatSpec with ShouldMatchers {
  val referenceApi = new File("reference-api")

  lazy val json = io.Source.fromFile(new File(referenceApi, "api.json"))
    .getLines.mkString("\n")

  def genCode(code: => String, path: String): Unit = {
    val file = new File(referenceApi, path)
    file.getParentFile.mkdirs
    val pw = new PrintWriter(file)
    try pw.println(code)
    finally pw.close()
  }

  "ReferenceApi" should "generate working code" in {
    genCode(
      Play2RouteGenerator(ServiceDescription(json)).generate.get,
      "conf/routes"
    )
    genCode(Play2ClientGenerator(json), "app/Play2Client.scala")
    genCode(ScalaCheckGenerators(json), "test/ScalaCheck.scala")
    genCode(
      RubyGemGenerator(ServiceDescription(json)).generate,
      "ruby/client.rb"
    )

    Process(
      Seq("sbt", "test"),
      cwd = referenceApi
    ).! should be(0)
  }
}
