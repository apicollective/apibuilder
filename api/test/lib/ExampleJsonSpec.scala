package lib

import com.bryzek.apidoc.spec.v0.models._
import com.bryzek.apidoc.spec.v0.models.json._
import org.scalatest.{FunSpec, ShouldMatchers}
import play.api.libs.json.Json

class ExampleJsonSpec extends FunSpec with ShouldMatchers with util.TestApplication {

  private[this] lazy val service = TestHelper.readService("../spec/apidoc-spec.json")
  private[this] lazy val exampleAll = ExampleJson.allFields(service)
  private[this] lazy val exampleMinimal = ExampleJson.requiredFieldsOnly(service)

  it("simple model") {
    val js = exampleAll.sample("info")
    val info = Json.parse(js.toString()).as[Info]
    info.license should be(Some(License("MIT", Some("http://opensource.org/licenses/MIT"))))
    info.contact should be(Some(
      Contact(
        name = Some("Michael Bryzek"),
        url = Some("http://www.apidoc.me"),
        email = Some("michael@test.apidoc.me")
      )
    ))
  }

  it("simple model w/ enum") {
    val js = exampleAll.sample("parameter")
    val param = Json.parse(js.toString()).as[Parameter]
    param.name.startsWith("lorem") should be(true)
    param.location should be(ParameterLocation.all.head)
    param.default.isDefined should be(true)
    param.minimum should be(Some(1))
    param.maximum should be(Some(1))
  }

  it("simple model - minimal fields") {
    val js = exampleMinimal.sample("parameter")
    val param = Json.parse(js.toString()).as[Parameter]
    param.name.startsWith("lorem") should be(true)
    param.location should be(ParameterLocation.all.head)
    param.default.isDefined should be(false)
    param.minimum.isDefined should be(false)
    param.maximum.isDefined should be(false)
  }

  it("uses default when present") {
    val js = exampleMinimal.sample("service")
    val service = Json.parse(js.toString()).as[Service]
    service.headers should be(Nil)
  }

}
