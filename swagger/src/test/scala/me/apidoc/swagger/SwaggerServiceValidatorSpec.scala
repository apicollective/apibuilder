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
    orgKey = "apidoc",
    orgNamespace = "me.apidoc",
    version = "0.0.2-dev"
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
          service.namespace should be("me.apidoc.swagger.petstore.v0")
          service.organization.key should be("apidoc")
          service.application.key should be("swagger-petstore")
          service.version should be("0.0.2-dev")
          service.baseUrl should be(Some("http://petstore.swagger.wordnik.com/api"))
          service.description should be(Some("A sample API that uses a petstore as an example to demonstrate features in the swagger-2.0 specification"))
          service.headers should be(Seq.empty)
          service.imports should be(Seq.empty)
          service.enums should be(Seq.empty)
          service.models.map(_.name) should be(Seq("pet", "newPet", "errorModel"))

          checkModel(
            service.models.find(_.name == "pet").get,
            Model(
              name = "pet",
              plural = "pets",
              description = Some("find more info here: https://helloreverb.com/about"),
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

          checkModel(
            service.models.find(_.name == "newPet").get,
            Model(
              name = "newPet",
              plural = "newPets",
              description = Some("find more info here: https://helloreverb.com/about"),
              fields = Seq(
                Field(
                  name = "id",
                  `type` = "long",
                  required = false
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

          checkModel(
            service.models.find(_.name == "errorModel").get,
            Model(
              name = "errorModel",
              plural = "errorModels",
              description = None,
              fields = Seq(
                Field(
                  name = "code",
                  `type` = "integer",
                  required = true
                ),
                Field(
                  name = "message",
                  `type` = "string",
                  required = true
                )
              )
            )
          )

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
