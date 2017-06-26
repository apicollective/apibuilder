package lib

import io.apibuilder.spec.v0.models._
import io.apibuilder.spec.v0.models.json._
import org.scalatest.{FunSpec, ShouldMatchers}
import play.api.libs.json.{JsNumber, Json}

class ExampleJsonSpec extends FunSpec with ShouldMatchers with util.TestApplication {

  private[this] lazy val service = TestHelper.readService("../spec/apibuilder-spec.json")
  private[this] lazy val exampleAll = ExampleJson.allFields(service)
  private[this] lazy val exampleMinimal = ExampleJson.requiredFieldsOnly(service)

  def buildServiceWithPrimitives(discriminator: Option[String]): Service = {
    val union = Union(
      name = "primitive",
      plural = "primitives",
      discriminator = discriminator,
      types = Seq(
        UnionType(`type` = "integer")
      )
    )

    service.copy(
      unions = service.unions ++ Seq(union)
    )
  }
  
  def buildServiceWithDimension(discriminator: Option[String]): Service = {
    val enum = Enum(
      name = "color",
      plural = "colors",
      values = Seq(
        EnumValue(name = "red")
      )
    )

    val union = Union(
      name = "dimension",
      plural = "dimension",
      discriminator = discriminator,
      types = Seq(
        UnionType(`type` = "color")
      )
    )

    service.copy(
      enums = service.enums ++ Seq(enum),
      unions = service.unions ++ Seq(union)
    )
  }

  def buildServiceWithParty(discriminator: Option[String]): Service = {
    val model = Model(
      name = "user",
      plural = "users",
      fields = Seq(
        Field(name = "name", `type` = "string", default = Some("Joe"), required = true)
      )
    )

    val union = Union(
      name = "party",
      plural = "parties",
      discriminator = discriminator,
      types = Seq(
        UnionType(`type` = "user")
      )
    )

    service.copy(
      models = service.models ++ Seq(model),
      unions = service.unions ++ Seq(union)
    )
  }  

  it("simple model") {
    val js = exampleAll.sample("info").get
    val info = Json.parse(js.toString()).as[Info]
    info.license should be(Some(License("MIT", Some("http://opensource.org/licenses/MIT"))))
    info.contact should be(Some(
      Contact(
        name = Some("Michael Bryzek"),
        url = Some("https://www.apibuilder.io"),
        email = Some("michael@test.apibuilder.io")
      )
    ))
  }

  it("simple model w/ enum") {
    val js = exampleAll.sample("parameter").get
    val param = Json.parse(js.toString()).as[Parameter]
    param.name.startsWith("lorem") should be(true)
    param.location should be(ParameterLocation.all.head)
    param.default.isDefined should be(true)
    param.minimum should be(Some(1))
    param.maximum should be(Some(1))
  }

  it("simple model - minimal fields") {
    val js = exampleMinimal.sample("parameter").get
    val param = Json.parse(js.toString()).as[Parameter]
    param.name.startsWith("lorem") should be(true)
    param.location should be(ParameterLocation.all.head)
    param.default.isDefined should be(false)
    param.minimum.isDefined should be(false)
    param.maximum.isDefined should be(false)
  }

  it("uses default when present") {
    val js = exampleMinimal.sample("service").get
    val service = Json.parse(js.toString()).as[Service]
    service.headers should be(Nil)
  }

  it("unknown type") {
    exampleMinimal.sample("foo") should be(None)
  }

  it("union type (no discriminator) containing an enum") {
    val svc = buildServiceWithDimension(discriminator = None)

    ExampleJson.allFields(svc).sample("dimension").get should equal(
      Json.obj(
        "color" -> "red"
      )
    )
  }

  it("union type (w/ discriminator) containing an enum") {
    val svc = buildServiceWithDimension(discriminator = Some("discriminator"))

    ExampleJson.allFields(svc).sample("dimension").get should equal(
      Json.obj(
        "discriminator" -> "color",
        "value" -> "red"
      )
    )
  }

  it("union type (no discriminator) containing a model") {
    val svc = buildServiceWithParty(discriminator = None)

    ExampleJson.allFields(svc).sample("user").get should equal(
      Json.obj(
        "user" -> Json.obj(
          "name" -> "Joe"
        )
      )
    )
  }

  it("union type (w/ discriminator) containing a model") {
    val svc = buildServiceWithParty(discriminator = Some("discriminator"))

    ExampleJson.allFields(svc).sample("user").get should equal(
      Json.obj(
        "discriminator" -> "user",
        "name" -> "Joe"
      )
    )
  }

  it("union type (no discriminator) containing a primitive") {
    val svc = buildServiceWithPrimitives(discriminator = None)

    ExampleJson.allFields(svc).sample("primitive").get should equal(
      Json.obj(
        "value" -> 1
      )
    )
  }

  it("union type (w/ discriminator) containing a primitive") {
    val svc = buildServiceWithPrimitives(discriminator = Some("discriminator"))

    ExampleJson.allFields(svc).sample("primitive").get should equal(
      Json.obj(
        "discriminator" -> "primitive",
        "value" -> 1
      )
    )
  }
  
}
