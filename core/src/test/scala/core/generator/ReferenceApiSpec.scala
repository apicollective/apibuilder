package core.generator

import java.io.File
import java.io.PrintWriter

import scala.sys.process._

import core._

import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers

class ReferenceApiSpec extends FlatSpec with ShouldMatchers {
  "ReferenceApi" should "generate working code" in {
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
      "ruby/client.rb"
    )

    Process(
      Seq("sbt", "test"),
      cwd = referenceApi
    ).! should be(0)

    val ruby = new File(referenceApi, "ruby")
    val bundle = Process("rbenv which bundle", cwd = ruby).!!.trim
    println(s"bundle executable is: $bundle")

    Process(
      Seq(bundle, "install", "--binstubs=bin", "--path=gems"),
      cwd = ruby
    ).! should be(0)

    Process(
      Seq("bin/rspec", "client_spec.rb"),
      cwd = ruby
    ).! should be(0)
  }
}
