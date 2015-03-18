package me.apidoc.swagger

import lib.{ServiceConfiguration, UrlKey}
import java.io.File
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets
import scala.collection.JavaConversions._
import play.api.libs.json.{Json, JsArray, JsObject, JsString, JsValue}
import java.util.UUID

import io.swagger.parser.SwaggerParser
import com.wordnik.swagger.models.Swagger

import lib.Text
import com.gilt.apidoc.spec.v0.models._

case class Parser(config: ServiceConfiguration) {

  def parse(
    path: File
  ): Service = {
    val swagger = new SwaggerParser().read(path.toString)
    println(swagger)
    val methods = swagger.getClass().getMethods().foreach { m =>
      println("METHOD: " + m)
    }

    println("swagger version: " + swagger.getSwagger())

    val info = swagger.getInfo()
    println("info:")
    println(" - title: " + info.getTitle())
    println(" - description: " + info.getDescription())
    println(" - termsOfServiceUrl: " + info.getTermsOfService())
    println(" - contact:")
    println("   - name: " + info.getContact().getName())
    println("   - url: " + info.getContact().getUrl())
    println("   - email: " + info.getContact().getEmail())
    println(" - license: " + info.getLicense())
    println("   - name: " + info.getLicense().getName())
    println("   - url: " + info.getLicense().getUrl())
    println(" - version: " + info.getVersion())

    println("host: " + swagger.getHost())
    println("basePath: " + swagger.getBasePath())
    println("schemes: " + toArray(swagger.getSchemes()).mkString(", "))
    val baseUrl = swagger.getSchemes.headOption.map(_.toString.toLowerCase).getOrElse("http") + "//" + swagger.getHost() + swagger.getBasePath()
    println("baseUrl: " + baseUrl)

    println("consumes: " + toArray(swagger.getConsumes()).mkString(", "))
    println("produces: " + toArray(swagger.getProduces).mkString(", "))

    swagger.getDefinitions.foreach { d =>
      println("definition: " + d)
    }

    // We're at 'path' - https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md

    sys.error("TODO")
  }

  private def toArray[T](values: java.util.List[T]): Seq[T] = {
    if (values == null) {
      Nil
    } else {
      values
    }
  }

  def parseString(
    contents: String
  ): Service = {
    val tmpPath = "/tmp/apidoc.swagger.tmp." + UUID.randomUUID.toString + ".json"
    writeToFile(tmpPath, contents)
    parse(new File(tmpPath))
  }

  private def writeToFile(path: String, contents: String) {
    val outputPath = Paths.get(path)
    val bytes = contents.getBytes(StandardCharsets.UTF_8)
    Files.write(outputPath, bytes)
  }

}
