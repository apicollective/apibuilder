package lib

import com.bryzek.apidoc.spec.v0.models._
import com.bryzek.apidoc.spec.v0.models.json._
import org.scalatest.{FunSpec, ShouldMatchers}
import play.api.libs.json.Json

class ExampleJsonSpec extends FunSpec with ShouldMatchers with util.TestApplication {

  private[this] lazy val service = TestHelper.readService("../spec/apidoc-spec.json")
  private[this] lazy val example = ExampleJson(service)

  it("simple model") {
    val js = example.sample("info")
    val info = Json.parse(js.toString()).as[Info]
    println(info)
    info.license should be(Some(License("MIT", Some("http://opensource.org/licenses/MIT"))))
    info.contact should be(Some(
      Contact(
        name = Some("Michael Bryzek"),
        url = Some("adf,"),
        email = Some("ASDF")
      )
    ))
  }

}
