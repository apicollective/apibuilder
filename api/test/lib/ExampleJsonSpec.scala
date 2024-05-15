package lib

import io.apibuilder.spec.v0.models._
import io.apibuilder.spec.v0.models.json._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsNull, JsNumber, JsString, Json}

class ExampleJsonSpec  extends PlaySpec
  with GuiceOneAppPerSuite
  with db.Helpers
  with TestHelper
{
  import ServiceBuilder._

  private[this] lazy val service: Service = readService("../spec/apibuilder-spec.json")
  private[this] lazy val exampleAll = ExampleJson.allFields(service)
  private[this] lazy val exampleMinimal = ExampleJson.requiredFieldsOnly(service)

  "simple model" in {
    val js = exampleAll.sample("info").get
    val info = Json.parse(js.toString()).as[Info]
    info.license must be(Some(License("MIT", Some("http://opensource.org/licenses/MIT"))))
    info.contact must be(Some(
      Contact(
        name = Some("Michael Bryzek"),
        url = Some("https://www.apibuilder.io"),
        email = Some("michael@test.apibuilder.io")
      )
    ))
  }

  "simple model w/ enum" in {
    val js = exampleAll.sample("parameter").get
    val param = Json.parse(js.toString()).as[Parameter]
    param.name.startsWith("lorem") must be(true)
    param.location must be(ParameterLocation.all.head)
    param.default.isDefined must be(true)
    param.minimum must be(Some(1))
    param.maximum must be(Some(1))
  }

  "simple model - minimal fields" in {
    val js = exampleMinimal.sample("parameter").get
    val param = Json.parse(js.toString()).as[Parameter]
    param.name.startsWith("lorem") must be(true)
    param.location must be(ParameterLocation.all.head)
    param.default.isDefined must be(false)
    param.minimum.isDefined must be(false)
    param.maximum.isDefined must be(false)
  }

  "uses default when present" in {
    val js = exampleMinimal.sample("service").get
    val service = Json.parse(js.toString()).as[Service]
    service.headers must be(Nil)
  }

  "unknown type" in {
    exampleMinimal.sample("foo") must be(None)
  }

  "unknown union" in {
    exampleMinimal.sample("foo", Some("bar")) must be(None)
  }

  "recursive model" in {
    val svc = service.withModel("user", _.withField("user", "user"))

    ExampleJson.allFields(svc).sample("user").get must equal(
      Json.obj(
        "user" -> Json.obj(
          "user" -> JsNull
        )
      )
    )
  }

  "recursive union without discriminator" in {
    val svc = service.withModel("human", _.withField("user", "user")).withUnion("user", _.withType("human"))

    ExampleJson.allFields(svc).sample("user").get must equal(
      Json.obj(
        "human" -> Json.obj(
          "user" -> Json.obj(
            "human" -> Json.obj(
              "user" -> JsNull
            )
          )
        )
      )
    )
  }

  "recursive union with discriminator" in {
    val svc = service.withModel("human", _.withField("user", "user")).withUnion("user", _.withType("human"), Some("discriminator"))

    ExampleJson.allFields(svc).sample("user").get must equal(
      Json.obj(
        "discriminator" -> "human",
        "user" -> Json.obj(
          "discriminator" -> "human",
          "user" -> JsNull
        )
      )
    )
  }

  "unknown union type" in {
    val svc = service.withUnion("primitive", _.withType("integer"))
    ExampleJson.allFields(svc).sample("primitive", Some("bar")) must be(None)
  }

  "specific union type" in {
    val svc = service.withUnion("primitive", _.withType("string").withType("integer"))
    ExampleJson.allFields(svc).sample("primitive", Some("integer")).get must equal(
      Json.obj(
        "integer" -> Json.obj("value" -> 1)
      )
    )
  }

  "enum" in {
    val svc = service.withEnum("color", _.withValue("red"))

    ExampleJson.allFields(svc).sample("color").get must equal(
      JsString("red")
    )
  }

  "enum without values" in {
    val svc = service.withEnum("color")

    ExampleJson.allFields(svc).sample("color").get must equal(
      JsString("undefined")
    )
  }

  "enum with custom value" in {
    val svc = service.withEnum("color", _.withValue("red", Some("blue")))

    ExampleJson.allFields(svc).sample("color").get must equal(
      JsString("blue")
    )
  }

  "union type (no discriminator) containing an enum" in {
    val svc = service.withEnum("color", _.withValue("red")).withUnion("dimension", _.withType("color"))

    ExampleJson.allFields(svc).sample("color").get must equal(
      JsString("red")
    )

    ExampleJson.allFields(svc).sample("dimension").get must equal(
      Json.obj(
        "color" -> "red"
      )
    )
  }

  "union type (no discriminator but with discriminator value) containing an enum" in {
    val svc = service.withEnum("color", _.withValue("red")).withUnion("dimension", _.withType("color", Some("farbe")))

    ExampleJson.allFields(svc).sample("dimension").get must equal(
      Json.obj(
        "farbe" -> "red"
      )
    )
  }

  "union type (w/ discriminator) containing an enum" in {
    val svc = service.withEnum("color", _.withValue("red")).withUnion("dimension", _.withType("color"), Some("discriminator"))

    ExampleJson.allFields(svc).sample("color").get must equal(
      JsString("red")
    )

    ExampleJson.allFields(svc).sample("dimension").get must equal(
      Json.obj(
        "discriminator" -> "color",
        "value" -> "red"
      )
    )
  }

  "union type (w/ discriminator) strips namespace off enum" in {
    val svc = service.withEnum("com.acme.color", _.withValue("red")).withUnion("dimension", _.withType("com.acme.color"), Some("discriminator"))

    ExampleJson.allFields(svc).sample("dimension").get must equal(
      Json.obj(
        "discriminator" -> "color",
        "value" -> "red"
      )
    )
  }

  "union type (w/ discriminator and discriminator value) containing an enum" in {
    val svc = service.withEnum("color", _.withValue("red")).withUnion("dimension", _.withType("color", Some("farbe")), Some("discriminator"))

    ExampleJson.allFields(svc).sample("dimension").get must equal(
      Json.obj(
        "discriminator" -> "farbe",
        "value" -> "red"
      )
    )
  }

  "union type (no discriminator) containing an enum with custom value" in {
    val svc = service.withEnum("color", _.withValue("red", Some("rouge"))).withUnion("dimension", _.withType("color"))

    ExampleJson.allFields(svc).sample("dimension").get must equal(
      Json.obj(
        "color" -> "rouge"
      )
    )
  }

  "union type (w/ discriminator) containing an enum with custom value" in {
    val svc = service.withEnum("color", _.withValue("red", Some("rouge"))).withUnion("dimension", _.withType("color"), Some("discriminator"))

    ExampleJson.allFields(svc).sample("dimension").get must equal(
      Json.obj(
        "discriminator" -> "color",
        "value" -> "rouge"
      )
    )
  }

  "union type (w/ discriminator and discriminator value) containing an enum with custom value" in {
    val svc = service.withEnum("color", _.withValue("red", Some("rouge"))).withUnion("dimension", _.withType("color", Some("farbe")), Some("discriminator"))

    ExampleJson.allFields(svc).sample("dimension").get must equal(
      Json.obj(
        "discriminator" -> "farbe",
        "value" -> "rouge"
      )
    )
  }

  "union type (no discriminator) containing a model" in {
    val svc = service.withModel("user", _.withField("name", "string", Some("Joe"))).withUnion("party", _.withType("user"))

    ExampleJson.allFields(svc).sample("user").get must equal(
      Json.obj(
        "name" -> "Joe"
      )
    )

    ExampleJson.allFields(svc).sample("party").get must equal(
      Json.obj(
        "user" -> Json.obj(
          "name" -> "Joe"
        )
      )
    )
  }

  "union type (no discriminator) containing nested types" in {
    val svc = service
      .withModel("inner", _.withField("int", "integer"))
      .withModel("outer", _.withField("nested", "inner").withField("strs", "[string]", Some("[\"foo\"]")))
      .withUnion("testunion", _.withType("outer"))

    ExampleJson.allFields(svc).sample("testunion").get must equal(
      Json.obj(
        "outer" -> Json.obj(
          "nested" -> Json.obj(
            "int" -> JsNumber(1)
          ),
          "strs" -> Json.arr("foo")
        )
      )
    )
  }

  "union type (no discriminator but with discriminator value) containing a model" in {
    val svc = service.withModel("user", _.withField("name", "string", Some("Joe"))).withUnion("party", _.withType("user", Some("usr")))

    ExampleJson.allFields(svc).sample("party").get must equal(
      Json.obj(
        "usr" -> Json.obj(
          "name" -> "Joe"
        )
      )
    )
  }

  "union type (w/ discriminator) containing a model" in {
    val svc = service.withModel("user", _.withField("name", "string", Some("Joe"))).withUnion("party", _.withType("user"), Some("discriminator"))

    ExampleJson.allFields(svc).sample("user").get must equal(
      Json.obj(
        "name" -> "Joe"
      )
    )

    ExampleJson.allFields(svc).sample("party").get must equal(
      Json.obj(
        "discriminator" -> "user",
        "name" -> "Joe"
      )
    )
  }

  "union type (w/ discriminator) strips namespace off model" in {
    val svc = service.withModel("com.acmecorp.user", _.withField("name", "string", Some("Joe"))).withUnion("party", _.withType("com.acmecorp.user"), Some("discriminator"))

    ExampleJson.allFields(svc).sample("party").get must equal(
      Json.obj(
        "discriminator" -> "user",
        "name" -> "Joe"
      )
    )
  }

  "union type (w/ discriminator) containing nested types" in {
    val svc = service
      .withModel("inner", _.withField("int", "integer"))
      .withModel("outer", _.withField("nested", "inner").withField("strs", "[string]", Some("[\"foo\"]")))
      .withUnion("testunion", _.withType("outer"), Some("discriminator"))

    ExampleJson.allFields(svc).sample("testunion").get must equal(
      Json.obj(
        "discriminator" -> "outer",
        "nested" -> Json.obj(
          "int" -> JsNumber(1)
        ),
        "strs" -> Json.arr("foo")
      )
    )
  }

  "union type (w/ discriminator and discriminator value) containing a model" in {
    val svc = service.withModel("user", _.withField("name", "string", Some("Joe"))).withUnion("party", _.withType("user", Some("usr")), Some("discriminator"))

    ExampleJson.allFields(svc).sample("party").get must equal(
      Json.obj(
        "discriminator" -> "usr",
        "name" -> "Joe"
      )
    )
  }

  "union type (no discriminator) containing a primitive" in {
    val svc = service.withUnion("primitive", _.withType("integer"))

    ExampleJson.allFields(svc).sample("primitive").get must equal(
      Json.obj(
        "integer" -> Json.obj("value" -> 1)
      )
    )
  }

  "union type (no discriminator but with discriminator value) containing a primitive" in {
    val svc = service.withUnion("primitive", _.withType("integer", Some("int")))

    ExampleJson.allFields(svc).sample("primitive").get must equal(
      Json.obj(
        "int" -> Json.obj("value" -> 1)
      )
    )
  }

  "union type (w/ discriminator) containing a primitive" in {
    val svc = service.withUnion("primitive", _.withType("integer"), Some("discriminator"))

    ExampleJson.allFields(svc).sample("primitive").get must equal(
      Json.obj(
        "discriminator" -> "integer",
        "value" -> 1
      )
    )
  }

  "union type (w/ discriminator and discriminator value) containing a primitive" in {
    val svc = service.withUnion("primitive", _.withType("integer", Some("int")), Some("discriminator"))

    ExampleJson.allFields(svc).sample("primitive").get must equal(
      Json.obj(
        "discriminator" -> "int",
        "value" -> 1
      )
    )
  }

  "union without types" in {
    val svc = service.withUnion("lonely")

    ExampleJson.allFields(svc).sample("lonely").get must equal(
      Json.obj()
    )
  }

  "model with json field" in {
    val svc = service.withModel("foo", _.withField("bar", "json"))

    ExampleJson.allFields(svc).sample("foo").get must equal(
      Json.obj("bar" -> JsNull)
    )
  }

  "union type (no discriminator) containing a json type" in {
    val svc = service.withUnion("primitive", _.withType("json"))

    ExampleJson.allFields(svc).sample("primitive").get must equal(
      Json.obj("json" -> Json.obj("value" -> JsNull))
    )
  }

  "union type (w/ discriminator) containing a json type" in {
    val svc = service.withUnion("primitive", _.withType("json"), Some("discriminator"))

    ExampleJson.allFields(svc).sample("primitive").get must equal(
      Json.obj("discriminator" -> "json", "value" -> JsNull)
    )
  }

}
