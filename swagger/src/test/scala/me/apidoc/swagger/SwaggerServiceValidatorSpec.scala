package me.apidoc.swagger

import com.gilt.apidoc.spec.v0.models.{Field, Model}
import lib.ServiceConfiguration
import org.scalatest.{FunSpec, Matchers}

class SwaggerServiceValidatorSpec extends FunSpec with Matchers {

  private def readFile(path: String): String = {
    scala.io.Source.fromFile(path).getLines.mkString("\n")
  }

  private def printRequired(value: Boolean): String = {
    value match {
      case true => "(required)"
      case false => "(optional)"
    }
  }

  private def checkModel(actual: Model, target: Model) {
    actual.name should be(target.name)
    actual.plural should be(target.plural)
    actual.description should be(target.description)
    actual.fields.map(_.name) should be(target.fields.map(_.name))
    actual.fields.foreach { f =>
      checkField(f, target.fields.find(_.name == f.name).get)
    }
  }

  private def checkField(actual: Field, target: Field) {
    actual.name should be(target.name)
    actual.`type` should be(target.`type`)
    actual.description should be(target.description)
    actual.deprecation should be(target.deprecation)
    actual.default should be(target.default)
    actual.required should be(target.required)
    actual.minimum should be(target.minimum)
    actual.maximum should be(target.maximum)
    actual.example should be(target.example)
  }

  val config = ServiceConfiguration(
    orgKey = "gilt",
    orgNamespace = "com.gilt",
    version = "0.0.1-dev"
  )

  it("parses") {
    //val files = Seq("petstore-expanded.json", "petstore-simple.json", "petstore.json", "petstore-minimal.json", "petstore-with-external-docs.json")
    val files = Seq("petstore-with-external-docs.json")
    files.foreach { filename =>
      val path = s"swagger/test/resources/$filename"
      println(s"Reading file[$path]")
      SwaggerServiceValidator(config, readFile(path)).validate match {
        case Left(errors) => {
          fail(s"Service validation failed for path[$path]: "  + errors.mkString(", "))
        }
        case Right(service) => {
          service.name should be("Swagger Petstore")
          service.models.map(_.name) should be(Seq("pet", "newPet", "errorModel"))

          checkModel(
            service.models.find(_.name == "pet").get,
            Model(
              name = "pet",
              plural = "pets",
              fields = Seq(
                Field(
                  name = "id",
                  `type` = "long",
                  required = true
                ),
                Field(
                  name = "name",
                  `type` = "string",
                  required = true
                ),
                Field(
                  name = "tag",
                  `type` = "string",
                  required = false
                )
              )
            )
          )

          println("No errors.")
          println("Service: " + service.name)
          service.models.foreach { m =>
            println(s" Model ${m.name}")
            m.fields.foreach { f =>
              println(s"   - Field ${f.name}, type: ${f.`type`} ${printRequired(f.required)}")
            }
          }

          service.resources.foreach { r =>
            println(s" Resource ${r.`type`}")
            r.operations.foreach { op =>
              println(s"  ${op.method} ${op.path}")

              println(s"   body:")
              op.body match {
                case None => {
                  println("    none")
                }
                case Some(b) => {
                  println(s"    ${b.`type`}")
                }
              }
              
              println(s"   parameters:")
              op.parameters match {
                case Nil => {
                  println("    none")
                }
                case params => {
                  params.foreach { p =>
                    println(s"    ${p.name}: ${p.`type`} (${p.location}) ${printRequired(p.required)}")
                  }
                }
              }

              println(s"   responses:")
              op.responses.foreach { r =>
                println(s"    ${r.code}: ${r.`type`}")
              }
            }
          }
        }
      }
    }
  }
}
