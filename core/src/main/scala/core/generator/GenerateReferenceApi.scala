package core.generator

import java.io.File
import java.io.PrintWriter

import scala.sys.process._

import core._

object GenerateReferenceApi extends App {
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

  val validator = ServiceDescriptionValidator(json)

  validator.validate() match {
    case Left(errors) =>
      println("====== Begin Reference API validation errors:")
      errors.foreach(println)
      println("====== End Reference API validation errors:")
      sys.error("refrence api.json is invalid!")
    case Right(serviceDescription) =>
      val serviceDescriptionWithUserAgent = serviceDescription.copy(userAgent = Some("apidoc gilt 0.0.1-reference"))
      genCode(
        Play2RouteGenerator(serviceDescriptionWithUserAgent).generate.get,
        "conf/routes"
      )
      genCode(Play2ClientGenerator.generate(PlayFrameworkVersions.V2_3_x, serviceDescriptionWithUserAgent), "app/Play2Client.scala")
      genCode(
        RubyClientGenerator(serviceDescriptionWithUserAgent).generate,
        "ruby/client.rb"
      )
  }
}
