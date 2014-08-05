package core.generator

import java.io.File
import java.io.PrintWriter

import scala.sys.process._

import core._

object GenerateReferenceApi extends App {
  val referenceApi = new File("reference-api")

  lazy val json = io.Source.fromFile(new File(referenceApi, "api.json"))
    .getLines.mkString("\n")

  lazy val serviceDescription = ServiceDescriptionValidator(json).serviceDescription.get

  def genCode(code: => String, path: String): Unit = {
    val file = new File(referenceApi, path)
    file.getParentFile.mkdirs
    val pw = new PrintWriter(file)
    try pw.println(code)
    finally pw.close()
  }

  val validator = ServiceDescriptionValidator(json)
  if (!validator.isValid) {
    println("====== Begin Reference API validation errors:")
    validator.errors.foreach(println)
    println("====== End Reference API validation errors:")
    sys.error("refrence api.json is invalid!")
  }
  genCode(
    Play2RouteGenerator(validator.serviceDescription.get).generate.get,
    "conf/routes"
  )
  genCode(Play2ClientGenerator.generate(PlayFrameworkVersions.V2_3_x, validator.serviceDescription.get, "apidoc gilt 0.0.1-reference"), "app/Play2Client.scala")
  genCode(
    RubyGemGenerator(validator.serviceDescription.get, "apidoc gilt 0.0.1-reference").generate,
    "ruby/client.rb"
  )
}
