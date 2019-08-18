package me.apidoc.swagger

import io.apibuilder.spec.v0.models.Method.{Delete, Get}
import io.apibuilder.spec.v0.models.ParameterLocation.{Path, Query}
import io.apibuilder.spec.v0.models.{EnumValue, _}
import lib.ServiceConfiguration
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.{JsArray, JsNull, JsObject, JsString}

class SwaggerServiceValidatorSpec extends FunSpec with Matchers {
  private val resourcesDir = "swagger/src/test/resources/"

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
    actual should be(target)
  }

  private def checkResource(actual: Resource, target: Resource): Unit = {
    actual should be(target)
  }

  private def checkEnum(actual: Enum, target: Enum): Unit = {
    actual should be(target)
  }

  val config = ServiceConfiguration(
    orgKey = "apidoc",
    orgNamespace = "me.apidoc",
    version = "0.0.2-dev"
  )

  describe("Swagger parser") {

    it("should parse petstore-enums.json") {
      val files = Seq("petstore-enums.json")
      files.foreach {
        filename =>
          val path = resourcesDir + filename
          println(s"Reading file[$path]")
          SwaggerServiceValidator(config, readFile(path)).validate match {
            case Left(errors) => {
              fail(s"Service validation failed for path[$path]: " + errors.mkString(", "))
            }
            case Right(service) => {
              service.name should be("Swagger Petstore")
              service.namespace should be("me.apidoc.swagger.petstore.v0")
              service.organization.key should be("apidoc")
              service.application.key should be("swagger-petstore")
              service.version should be("0.0.2-dev")
              service.baseUrl should be(Some("http://petstore.swagger.wordnik.com/api"))
              service.headers should be(Seq.empty)
              service.imports should be(Seq.empty)
              service.attributes should be (
                Seq(Attribute(
                  name = SwaggerData.AttributeName,
                  description = Some(SwaggerData.AttributeDescription),
                  value = JsObject(Seq(
                    ("schemes", JsArray(Seq(JsString("http")))),
                    ("host", JsString("petstore.swagger.wordnik.com")),
                    ("basePath", JsString("/api"))
                  ))),
                  Attribute(
                    name = "foo-service",
                    value = JsObject(Seq(("bar", JsString("service"))))
                  )))

              service.models.map(_.name).sorted should be(Seq("Error", "Pet"))
              checkModel(
                service.models.find(_.name == "Pet").get,
                Model(
                  name = "Pet",
                  plural = "Pets",
                  description = Some("Definition of a pet"),
                  fields = Seq(
                    Field(
                      name = "name",
                      `type` = "string",
                      required = true
                    ),
                    Field(
                      name = "email_address",
                      `type` = "string",
                      required = false
                    ),
                    Field(
                      name = "tag",
                      `type` = "string",
                      required = false,
                      attributes = Seq(Attribute(
                        name = "foo-field",
                        value = JsObject(Seq(("bar", JsString("field"))))
                      ))
                    ),
                    Field(
                      name = "additional_info",
                      `type` = "object",
                      required = false
                    ),
                    Field(
                      name = "id",
                      `type` = "long",
                      required = true
                    ),
                    Field(
                      name = "status",
                      `type` = "PetStatus",
                      required = false
                    ),
                    Field(
                      name = "html_description",
                      `type` = "string",
                      required = false
                    )
                  ),
                  attributes = Seq(Attribute(
                    name = "foo-model",
                    value = JsObject(Seq(("bar", JsString("model"))))
                  ))
                )
              )

              checkModel(
                service.models.find(_.name == "Error").get,
                Model(
                  name = "Error",
                  plural = "Errors",
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

              service.resources.size should be(1)
              checkResource(service.resources.find(_.`type` == "Pet").get,
                Resource(
                  `type` = "Pet",
                  plural = "Pets",
                  path = None,
                  description = None,
                  deprecation = None,
                  operations = Seq(
                    Operation(
                      method = Get,
                      path = "/pets/",
                      description = Some("find pets by name and status - as query params"),
                      deprecation = None,
                      body = None,
                      parameters = Seq(
                        Parameter(
                          name = "name",
                          `type` = "string",
                          location = Query,
                          description = None,
                          deprecation = None,
                          required = true,
                          default = None,
                          minimum = None,
                          maximum = None,
                          example = None),
                        Parameter(
                          name = "status",
                          `type` = "PetStatusGetQuery",
                          location = Query,
                          description = None,
                          deprecation = None,
                          required = true,
                          default = None,
                          minimum = None,
                          maximum = None,
                          example = None)
                      ),
                      responses = Seq(
                        Response(
                          code = ResponseCodeInt(200),
                          `type` = "[Pet]",
                          description = Some("find pet response"),
                          deprecation = None,
                          attributes = Some(Seq(Attribute(
                            name = "foo-response",
                            value = JsObject(Seq(("bar", JsString("response"))))
                          )))),
                        Response(
                          code = ResponseCodeOption.Default,
                          `type` = "Error",
                          description = Some("unexpected error"),
                          deprecation = None)
                      ),
                      attributes =  Seq(Attribute(
                        name = SwaggerData.AttributeName,
                        description = Some(SwaggerData.AttributeDescription),
                        value = JsObject(Seq(
                          ("summary", JsString("find pets by name and status - as query params"))
                        ))),
                        Attribute(
                          name = "foo-operation",
                          value = JsObject(Seq(("bar", JsString("operation"))))
                        ))),
                    Operation(
                      method = Get,
                      path = "/pets/:status",
                      description = Some("find pets by status - as a path param"),
                      deprecation = None,
                      body = None,
                      parameters = Seq(Parameter(
                        name = "status",
                        `type` = "PetStatusGetPath",
                        location = Path,
                        description = None,
                        deprecation = None,
                        required = true,
                        default = None,
                        minimum = None,
                        maximum = None,
                        example = None)
                      ),
                      responses = Seq(
                        Response(
                          code = ResponseCodeInt(200),
                          `type` = "[Pet]",
                          description = Some("find pet response"),
                          deprecation = None),
                        Response(
                          code = ResponseCodeOption.Default,
                          `type` = "Error",
                          description = Some("unexpected error"),
                          deprecation = None)
                      ),
                      attributes =  Seq(Attribute(
                        name = SwaggerData.AttributeName,
                        description = Some(SwaggerData.AttributeDescription),
                        value = JsObject(Seq(
                          ("summary", JsString("find pets by status - as a path param"))
                        ))))),
                    Operation(
                      method = Delete,
                      path = "/pets/:status",
                      description = Some("delete pets by status - as a path param"),
                      deprecation = None,
                      body = None,
                      parameters = Seq(Parameter(
                        name = "status",
                        `type` = "PetStatusDeletePath",
                        location = Path,
                        description = None,
                        deprecation = None,
                        required = true,
                        default = None,
                        minimum = None,
                        maximum = None,
                        example = None)
                      ),
                      responses = Seq(
                        Response(
                          code = ResponseCodeInt(204),
                          `type` = "unit",
                          description = Some("pets deleted - no response body content"),
                          deprecation = None),
                        Response(
                          code = ResponseCodeOption.Default,
                          `type` = "Error",
                          description = Some("unexpected error"),
                          deprecation = None)
                      ),
                      attributes =  Seq(Attribute(
                        name = SwaggerData.AttributeName,
                        description = Some(SwaggerData.AttributeDescription),
                        value = JsObject(Seq(
                          ("tags", JsArray(Seq(JsString("tag1"), JsString("tag2")))),
                          ("summary", JsString("delete pets by status - as a path param")),
                          ("operationId", JsString("deletePet"))
                        )))))
                  ),
                  attributes = Seq())
                )

              service.enums.size should be(4)
              checkEnum(service.enums.find(_.name == "PetStatus").get,
                Enum(
                  name = "PetStatus",
                  plural = "PetStatuses",
                  description = None,
                  deprecation = None,
                  values = Seq(
                    EnumValue(name = "available", description = None, deprecation = None, attributes = Seq()),
                    EnumValue(name = "pending", description = None, deprecation = None, attributes = Seq()),
                    EnumValue(name = "sold", description = None, deprecation = None, attributes = Seq())),
                  attributes = Seq()
                ))
              checkEnum(service.enums.find(_.name == "PetStatusGetQuery").get,
                Enum(
                  name = "PetStatusGetQuery",
                  plural = "PetStatusGetQueries",
                  description = None,
                  deprecation = None,
                  values = Seq(
                    EnumValue(name = "available", description = None, deprecation = None, attributes = Seq()),
                    EnumValue(name = "pending", description = None, deprecation = None, attributes = Seq()),
                    EnumValue(name = "sold", description = None, deprecation = None, attributes = Seq())),
                  attributes = Seq()
                ))
              checkEnum(service.enums.find(_.name == "PetStatusGetPath").get,
                Enum(
                  name = "PetStatusGetPath",
                  plural = "PetStatusGetPaths",
                  description = None,
                  deprecation = None,
                  values = Seq(
                    EnumValue(name = "available", description = None, deprecation = None, attributes = Seq()),
                    EnumValue(name = "pending", description = None, deprecation = None, attributes = Seq()),
                    EnumValue(name = "sold", description = None, deprecation = None, attributes = Seq())),
                  attributes = Seq()
                ))
              checkEnum(service.enums.find(_.name == "PetStatusDeletePath").get,
                Enum(
                  name = "PetStatusDeletePath",
                  plural = "PetStatusDeletePaths",
                  description = None,
                  deprecation = None,
                  values = Seq(
                    EnumValue(name = "available", description = None, deprecation = None, attributes = Seq()),
                    EnumValue(name = "pending", description = None, deprecation = None, attributes = Seq()),
                    EnumValue(name = "sold", description = None, deprecation = None, attributes = Seq())),
                  attributes = Seq()
                ))
            }
          }
      }
    }

    it("should parse petstore-enums-ref.json") {
      val files = Seq("petstore-enums-ref.json")
      files.foreach {
        filename =>
          val path = resourcesDir + filename
          println(s"Reading file[$path]")
          SwaggerServiceValidator(config, readFile(path)).validate match {
            case Left(errors) => {
              fail(s"Service validation failed for path[$path]: " + errors.mkString(", "))
            }
            case Right(service) => {
              service.name should be("Swagger Petstore")
              service.namespace should be("me.apidoc.swagger.petstore.v0")
              service.organization.key should be("apidoc")
              service.application.key should be("swagger-petstore")
              service.version should be("0.0.2-dev")
              service.baseUrl should be(Some("http://petstore.swagger.wordnik.com/api"))
              service.headers should be(Seq.empty)
              service.imports should be(Seq.empty)
              service.attributes should be (
                Seq(Attribute(
                  name = SwaggerData.AttributeName,
                  description = Some(SwaggerData.AttributeDescription),
                  value = JsObject(Seq(
                    ("schemes", JsArray(Seq(JsString("http")))),
                    ("host", JsString("petstore.swagger.wordnik.com")),
                    ("basePath", JsString("/api"))
                  )))))

              service.models.map(_.name).sorted should be(Seq("Accessory", "Error", "Pet"))

              checkModel(
                service.models.find(_.name == "Pet").get,
                Model(
                  name = "Pet",
                  plural = "Pets",
                  description = Some("Definition of a pet"),
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
                    ),
                    Field(
                      name = "status",
                      `type` = "Status",
                      required = false
                    )
                  )
                )
              )

              checkModel(
                service.models.find(_.name == "Accessory").get,
                Model(
                  name = "Accessory",
                  plural = "Accessories",
                  description = Some("Accessory for a pet"),
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
                      name = "status",
                      `type` = "Status",
                      required = false
                    )
                  )
                )
              )

              checkModel(
                service.models.find(_.name == "Error").get,
                Model(
                  name = "Error",
                  plural = "Errors",
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

              service.resources.size should be(1)
              checkResource(service.resources.find(_.`type` == "Pet").get,
                Resource(
                  `type` = "Pet",
                  plural = "Pets",
                  path = None,
                  description = None,
                  deprecation = None,
                  operations = Seq(
                    Operation(
                      method = Get,
                      path = "/pets/:status",
                      description = Some("find pets by status - as a path param"),
                      deprecation = None,
                      body = None,
                      parameters = Seq(Parameter(
                        name = "status",
                        `type` = "PetStatusGetPath",
                        location = Path,
                        description = None,
                        deprecation = None,
                        required = true,
                        default = None,
                        minimum = None,
                        maximum = None,
                        example = None)
                      ),
                      responses = Seq(
                        Response(
                          code = ResponseCodeInt(200),
                          `type` = "[Pet]",
                          description = Some("find pet response"),
                          deprecation = None),
                        Response(
                          code = ResponseCodeOption.Default,
                          `type` = "Error",
                          description = Some("unexpected error"),
                          deprecation = None)
                      ),
                      attributes =  Seq(Attribute(
                        name = SwaggerData.AttributeName,
                        description = Some(SwaggerData.AttributeDescription),
                        value = JsObject(Seq(
                          ("summary", JsString("find pets by status - as a path param"))
                        ))))),
                    Operation(
                      method = Get,
                      path = "/pets/",
                      description = Some("find pets by name and status - as query params"),
                      deprecation = None,
                      body = None,
                      parameters = Seq(
                        Parameter(
                          name = "name",
                          `type` = "string",
                          location = Query,
                          description = None,
                          deprecation = None,
                          required = true,
                          default = None,
                          minimum = None,
                          maximum = None,
                          example = None),
                        Parameter(
                          name = "status",
                          `type` = "PetStatusGetQuery",
                          location = Query,
                          description = None,
                          deprecation = None,
                          required = true,
                          default = None,
                          minimum = None,
                          maximum = None,
                          example = None)
                      ),
                      responses = Seq(
                        Response(
                          code = ResponseCodeInt(200),
                          `type` = "[Pet]",
                          description = Some("find pet response"),
                          deprecation = None),
                        Response(
                          code = ResponseCodeOption.Default,
                          `type` = "Error",
                          description = Some("unexpected error"),
                          deprecation = None)
                      ),
                      attributes =  Seq(Attribute(
                        name = SwaggerData.AttributeName,
                        description = Some(SwaggerData.AttributeDescription),
                        value = JsObject(Seq(
                          ("summary", JsString("find pets by name and status - as query params"))
                        ))))),
                  ),
                  attributes = Seq())
              )

              service.enums.size should be(3)
              checkEnum(service.enums.find(_.name == "Status").get,
                Enum(
                  name = "Status",
                  plural = "Statuses",
                  description = Some("Possible statuses of a pet"),
                  deprecation = None,
                  values = Seq(
                    EnumValue(name = "available", description = None, deprecation = None, attributes = Seq()),
                    EnumValue(name = "pending", description = None, deprecation = None, attributes = Seq()),
                    EnumValue(name = "sold", description = None, deprecation = None, attributes = Seq())),
                  attributes = Seq()
                ))
              checkEnum(service.enums.find(_.name == "PetStatusGetQuery").get,
                Enum(
                  name = "PetStatusGetQuery",
                  plural = "PetStatusGetQueries",
                  description = None,
                  deprecation = None,
                  values = Seq(
                    EnumValue(name = "available", description = None, deprecation = None, attributes = Seq()),
                    EnumValue(name = "pending", description = None, deprecation = None, attributes = Seq()),
                    EnumValue(name = "sold", description = None, deprecation = None, attributes = Seq())),
                  attributes = Seq()
                ))
              checkEnum(service.enums.find(_.name == "PetStatusGetPath").get,
                Enum(
                  name = "PetStatusGetPath",
                  plural = "PetStatusGetPaths",
                  description = None,
                  deprecation = None,
                  values = Seq(
                    EnumValue(name = "available", description = None, deprecation = None, attributes = Seq()),
                    EnumValue(name = "pending", description = None, deprecation = None, attributes = Seq()),
                    EnumValue(name = "sold", description = None, deprecation = None, attributes = Seq())),
                  attributes = Seq()
                ))
            }
          }
      }
    }

    it("should parse petstore-external-docs-example-security.json") {
      val files = Seq("petstore-external-docs-example-security.json")
      files.foreach {
        filename =>
          val path = resourcesDir + filename
          println(s"Reading file[$path]")
          SwaggerServiceValidator(config, readFile(path)).validate match {
            case Left(errors) => {
              fail(s"Service validation failed for path[$path]: " + errors.mkString(", "))
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
              service.models.map(_.name).sorted should be(Seq("errorModel", "newPet", "pet"))
              service.attributes should be (
                Seq(Attribute(
                  name = SwaggerData.AttributeName,
                  description = Some(SwaggerData.AttributeDescription),
                  value = JsObject(Seq(
                    ("externalDocs", JsObject(Seq(
                      ("description", JsString("find more info here")),
                      ("url", JsString("https://helloreverb.com/about"))))),
                    ("securityDefinitions", JsObject(Seq(
                      ("read", JsObject(Seq(
                        ("type", JsString("basic")),
                        ("description", JsString("Security def for read operations"))
                      ))),
                      ("write", JsObject(Seq(
                        ("type", JsString("basic")),
                        ("description", JsString("Security def for write operations"))
                      )))))),
                    ("security", JsArray(Seq(
                      JsObject(Seq(
                        ("scopes", JsNull),
                        ("read", JsArray())
                      ))))),
                    ("schemes", JsArray(Seq(JsString("http")))),
                    ("host", JsString("petstore.swagger.wordnik.com")),
                    ("basePath", JsString("/api"))
                  )))))

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
                      name = "guid",
                      `type` = "uuid",
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
                  ),
                  attributes = Seq(Attribute(
                    name = SwaggerData.AttributeName,
                    description = Some(SwaggerData.AttributeDescription),
                    value = JsObject(Seq(
                      ("externalDocs", JsObject(Seq(
                        ("description", JsString("find more info here")),
                        ("url", JsString("https://helloreverb.com/about"))))),
                      ("example", JsObject(Seq(
                        ("id", JsString("123")),
                        ("name", JsString("Storm")))))))))
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
                      name = "guid",
                      `type` = "uuid",
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
                  ),
                  attributes = Seq(Attribute(
                    name = SwaggerData.AttributeName,
                    description = Some(SwaggerData.AttributeDescription),
                    value = JsObject(Seq(
                      ("externalDocs", JsObject(Seq(
                        ("description", JsString("find more info here")),
                        ("url", JsString("https://helloreverb.com/about"))))),
                      ("example", JsObject(Seq(
                        ("id", JsString("123")),
                        ("name", JsString("Storm")))))))))
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

              service.resources.size shouldBe 1
              val resource = service.resources.head
              resource.`type` shouldBe ("pet")
              resource.operations.size shouldBe 4
              resource.operations.map(_.path).toSet shouldBe Set("/pets", "/pets/:id")

              service.resources.foreach {
                r =>
                  println(s" Resource ${r.`type`}")
                  r.operations.foreach {
                    op =>
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
                          params.foreach {
                            p =>
                              println(s"    ${p.name}: ${p.`type`} (${p.location}) ${printRequired(p.required)}")
                          }
                        }
                      }

                      println(s"   attributes:")
                      op.attributes match {
                        case Nil | Seq() => {
                          println("    none")
                        }
                        case attributes => {
                          attributes.foreach {
                            a =>
                              println(s"    ${a.name}: ${a.value}")
                          }
                        }
                      }

                      println(s"   responses:")
                      op.responses.foreach {
                        r =>
                          println(s"    ${r.code}: ${r.`type`}")
                      }
                  }
              }
            }
          }
      }
    }

    it("should parse petstore-with-external-docs-and-complex-path.json") {
      val files = Seq("petstore-with-external-docs-and-complex-path.json")
      files.foreach {
        filename =>
          val path = resourcesDir + filename
          println(s"Reading file[$path]")
          SwaggerServiceValidator(config, readFile(path)).validate match {
            case Left(errors) => {
              fail(s"Service validation failed for path[$path]: " + errors.mkString(", "))
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
              service.models.map(_.name).sorted should be(Seq("errorModel", "newPet", "pet"))
              service.attributes should be (
                Seq(Attribute(
                  name = SwaggerData.AttributeName,
                  description = Some(SwaggerData.AttributeDescription),
                  value = JsObject(Seq(
                    ("externalDocs", JsObject(Seq(
                      ("description", JsString("find more info here")),
                      ("url", JsString("https://helloreverb.com/about"))))),
                    ("schemes", JsArray(Seq(JsString("http")))),
                    ("host", JsString("petstore.swagger.wordnik.com")),
                    ("basePath", JsString("/api"))
                  )))))

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
                  ),
                  attributes = Seq(Attribute(
                    name = SwaggerData.AttributeName,
                    description = Some(SwaggerData.AttributeDescription),
                    value = JsObject(Seq(
                      ("externalDocs", JsObject(Seq(
                        ("description", JsString("find more info here")),
                        ("url", JsString("https://helloreverb.com/about")))))))))
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
                  ),
                  attributes = Seq(Attribute(
                    name = SwaggerData.AttributeName,
                    description = Some(SwaggerData.AttributeDescription),
                    value = JsObject(Seq(
                      ("externalDocs", JsObject(Seq(
                        ("description", JsString("find more info here")),
                        ("url", JsString("https://helloreverb.com/about")))))))))
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
                    ),
                    Field(
                      name = "timestamp",
                      `type` = "date-time-iso8601",
                      required = true
                    )
                  )
                )
              )

              service.resources.filter(_.`type` == "pet").flatMap(_.operations.map(_.path)).toSet shouldBe Set(
                "/pets/:id",
                "/store/:store_id/pets/:product_code"
              )

              service.resources.foreach {
                r =>
                  println(s" Resource ${r.`type`}")
                  r.operations.foreach {
                    op =>
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
                          params.foreach {
                            p =>
                              println(s"    ${p.name}: ${p.`type`} (${p.location}) ${printRequired(p.required)}")
                          }
                        }
                      }

                      println(s"   responses:")
                      op.responses.foreach {
                        r =>
                          println(s"    ${r.code}: ${r.`type`}")
                      }
                  }
              }
            }
          }
      }
    }

    it("should parse refs both in JSON and YAML file formats") {
      val files = Seq("refs.json", "refs.yaml")
      files.foreach {
        filename =>
          val path = resourcesDir + filename
          println(s"Reading file[$path]")
          assertRefsSpec(path)
      }
    }

    it("should not fail when no resource model can be found") {
      val files = Seq("no-resources.json")
      files.foreach {
        filename =>
          val path = resourcesDir + filename
          println(s"Reading file[$path]")
          SwaggerServiceValidator(config, readFile(path)).validate match {
            case Left(errors) => {
              fail(s"Service validation failed for path[$path]: " + errors.mkString(", "))
            }
            case Right(service) => {
              service.resources should be(Seq())
              service.models should be (Seq())
            }
          }
      }
    }
  }

  def assertRefsSpec(path: String) = {
    SwaggerServiceValidator(config, readFile(path)).validate match {
      case Left(errors) => {
        fail(s"Service validation failed for path[$path]: " + errors.mkString(", "))
      }
      case Right(service) => {
        service.name should be("Inventory API")
        service.namespace should be("me.apidoc.inventory.api.v0")
        service.organization.key should be("apidoc")
        service.application.key should be("inventory-api")
        service.version should be("0.0.2-dev")
        service.baseUrl should be(Some("https://api.company.com/v3"))
        service.description should be(Some("API to retrieve inventory information"))
        service.headers should be(Seq.empty)
        service.imports should be(Seq.empty)
        service.enums should be(Seq.empty)
        service.models.map(_.name).sorted should be(Seq("Body", "Error", "Inventory", "Message", "Messages", "Request", "Response", "Result", "Variant"))
        service.imports should be(Seq.empty)

        checkModel(
          service.models.find(_.name == "Response").get,
          Model(
            name = "Response",
            plural = "Responses",
            description = None,
            fields = Seq(
              Field(
                name = "request",
                `type` = "Request",
                required = true
              ),
              Field(
                name = "response",
                `type` = "Result",
                required = true
              ),
              Field(
                name = "errors",
                `type` = "Error",
                required = true
              )
            )
          )
        )

        checkModel(
          service.models.find(_.name == "Inventory").get,
          Model(
            name = "Inventory",
            plural = "Inventories",
            description = None,
            fields = Seq(
              Field(
                name = "help",
                `type` = "string",
                required = true
              ),
              Field(
                name = "messages",
                `type` = "Messages",
                required = true
              )
            )
          )
        )

        checkModel(
          service.models.find(_.name == "Messages").get,
          Model(
            name = "Messages",
            plural = "Messages",
            description = None,
            fields = Seq(
              Field(
                name = "product",
                `type` = "[Message]",
                required = false
              )
            )
          )
        )

        service.resources.foreach {
          r =>
            println(s" Resource ${r.`type`}")
            r.operations.foreach {
              op =>
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
                    params.foreach {
                      p =>
                        println(s"    ${p.name}: ${p.`type`} (${p.location}) ${printRequired(p.required)}")
                    }
                  }
                }

                println(s"   responses:")
                op.responses.foreach {
                  r =>
                    println(s"    ${r.code}: ${r.`type`}")
                }
            }
        }
      }
    }
  }
}
