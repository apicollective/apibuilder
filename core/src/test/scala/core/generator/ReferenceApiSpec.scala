package core.generator

import java.io.File
import java.io.PrintWriter

import scala.sys.process._

import core._

import org.scalatest.FlatSpec

class ReferenceApiSpec extends FlatSpec {
  "Play2ClientGenerator" should "generate scala" in {
    val referenceApi = new File("reference-api")
    val json = io.Source.fromFile(new File(referenceApi, "api.json"))
      .getLines.mkString("\n")

    def genCode(code: => String, path: String) {
      val file = new File(referenceApi, path)
      file.getParentFile.mkdirs
      val pw = new PrintWriter(file)
      try pw.println(code)
      finally pw.close()
    }

    genCode(
      Play2RouteGenerator(ServiceDescription(json)).generate.get,
      "conf/routes"
    )
    genCode(Play2ClientGenerator(json), "app/Play2Client.scala")
    genCode(ScalaCheckGenerators(json), "test/ScalaCheck.scala")
    genCode(
      RubyGemGenerator(ServiceDescription(json)).generate,
      "ruby/lib/client.rb"
    )

    assert(
      Process(
        Seq("sbt", "test"),
        cwd = referenceApi
      ).!  == 0,
      "scala tests failed"
    )

    assert(
      Process(
        // TODO this just checks syntax.
        // need to work out how to actually
        // test the ruby client
        Seq("ruby", "ruby/lib/client.rb"),
        cwd = referenceApi
      ).! == 0,
      "ruby tests failed"
    )
  }
}
