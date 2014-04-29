package core.generator.scala

import core.{ Datatype, Field, ServiceDescription, Resource }
import java.io.File
import java.io.PrintWriter

object SprayClientGenerator {
  def apply(service: ServiceDescription) = new SprayClientGenerator(service).generate
}

class SprayClientGenerator(service: ServiceDescription) {
  private val projectName = service.name.replaceAll("""\s+""", "-").toLowerCase

  private val packageName = projectName.replaceAll("-", "")

  private val buildSbt: String = {
"""
libraryDependencies ++= Seq(
  "io.spray" % "spray-client" % "1.2.1",
  "org.joda" % "joda-convert" % "1.6",
  "joda-time" % "joda-time" % "2.3"
)
"""
  }

  def generate: String = {
    val baseDir = new File(s"/web/${projectName}-client/")

    val clientPkg = new File(baseDir, s"src/main/scala/com/$packageName/client")
    val modelsPkg = new File(clientPkg, "models")

    clientPkg.mkdirs
    modelsPkg.mkdirs

    val buildSbtWriter = new PrintWriter(new File(baseDir, "build.sbt"))
    try buildSbtWriter.write(buildSbt)
    finally buildSbtWriter.close


    CaseClassGenerator(service).map { case (className, classSource) =>
      val pw = new PrintWriter(new File(modelsPkg, className + ".scala"))
      try pw.write(classSource)
      finally pw.close
    }

    baseDir.toString
  }
}
