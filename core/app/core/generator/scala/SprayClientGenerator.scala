package core.generator.scala

import core.{ Datatype, Field, ServiceDescription, Resource, Text }
import java.io.File
import java.io.PrintWriter

object SprayClientGenerator {
  def apply(service: ServiceDescription) = new SprayClientGenerator(service).generate
}

class SprayClientGenerator(service: ServiceDescription) {
  private val projectName = service.name.replaceAll("""\s+""", "-").toLowerCase

  private val packageName = projectName.replaceAll("-", "")

  def generate: String = {
    val baseDir = new File(s"/web/${projectName}-client/")

    val clientPkg = new File(baseDir, s"app/$packageName")

    clientPkg.mkdirs
    val pw = new PrintWriter(new File(clientPkg, "Client.scala"))
    try {
      pw.write {
s"""package $packageName

import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.functional.syntax._
"""
      }

      val caseClasses = CaseClassGenerator(service)

      val imports = caseClasses.flatMap(_.imports).distinct

      imports.map { imp =>
        pw.write(imp + "\n")
      }

      val jsonFormats = caseClasses.map { cc =>
        val className = cc.name
        pw.write(cc.src)
        JsonFormatDefs(cc)
      }

      pw.write {
s"""
object Client {
  val apiToken = sys.props.getOrElse(
    "$packageName.api.token",
    sys.error("API token must be provided")
  )

  val apiUrl = sys.props.getOrElse(
    "$packageName.api.url",
    sys.error("API URL must be provided")
  )

  def req(resource: String) = {
    import com.ning.http.client.Realm.AuthScheme
    val url = apiUrl + resource
    WS.url(url).withAuth(apiToken, "", AuthScheme.BASIC)
  }
"""
}
jsonFormats.foreach { c => pw.write(c.src) }
pw.write {
"""
}

object Tester extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Await
  import scala.concurrent.duration._

  import Client.jsonReadsItem
  import Client.jsonWritesItem

  val f = Client.req("/items").get().map {
    r => r.json.as[JsArray].value.map { _.as[Item] }
  }
  println(Json.prettyPrint(Json.toJson(Await.result(f, 1 seconds))))
}

trait Client {
  def resource: String
}
"""
      }

      ClientGenerator(service).map { case (className, classSource) =>
        pw.write(classSource)
      }
    }
    finally pw.close
    baseDir.toString
  }

// WARNING: This does not support recursive types right now. I think it is possible,
// but not worth the effort, since we don't have it as a use case.
  case class JsonFormatDefs(cc: ScalaCaseClass) extends Source {
    val jsonReadsBody: String = {
      val fieldMap = cc.fields.map(f => f.name -> f).toMap
      if (cc.fields.size > 1) {
        val inner = cc.resource.fields.map { field =>
          val scalaName = Text.underscoreToCamelCase(field.name)
          val scalaField = fieldMap(scalaName)
          if (scalaField.isOption) {
            s"""(__ \\ "${field.name}").readNullable[${scalaField.typeName}]"""
          } else {
            s"""(__ \\ "${field.name}").read[${scalaField.typeName}]"""
          }
        }.mkString("\n     and ")
s"""
    ($inner)(${cc.name})"""
      } else {
        val field = cc.fields.head
s"""
    new Reads[${cc.name}] {
      override def reads(json: JsValue) = {
        (json \\ "${field.originalField.name}").validate[${field.fullTypeName}].map { value =>
          new ${cc.name}(
            ${field.name} = value
          )
        }
      }
    }"""
      }
    }

    val jsonWritesBody: String = {
      val fieldMap = cc.fields.map(f => f.name -> f).toMap
      if (cc.fields.size > 1) {
        val inner = cc.resource.fields.map { field =>
          val scalaName = Text.underscoreToCamelCase(field.name)
          val scalaField = fieldMap(scalaName)
          if (scalaField.isOption) {
            s"""(__ \\ "${field.name}").writeNullable[${scalaField.typeName}]"""
          } else {
            s"""(__ \\ "${field.name}").write[${scalaField.typeName}]"""
          }
        }.mkString("\n     and ")
s"""
    ($inner)(unlift(${cc.name}.unapply))"""
      } else {
        val field = cc.fields.head
s"""
    new Writes[${cc.name}] {
      override def writes(value: ${cc.name}) = {
        Json.obj(
          "${field.originalField.name}" -> Json.toJson(value.${field.name})
        )
      }
    }"""
      }
    }

    override val src: String = {
s"""
// ${cc.name} JSON format
  implicit val jsonReads${cc.name}: Reads[${cc.name}] =$jsonReadsBody

  implicit val jsonWrites${cc.name}: Writes[${cc.name}] =$jsonWritesBody
"""
    }
  }
}
