package core.generator

import core.TestHelper
import org.scalatest.{ ShouldMatchers, FunSpec }

class Play2RouteGeneratorSpec extends FunSpec with ShouldMatchers {

  lazy val service = TestHelper.parseFile(s"api/api.json").serviceDescription.get
  lazy val userResource = service.resources.find { _.model.name == "user" }.getOrElse {
    sys.error("Could not find user resource")
  }

  it("GET w/ default path, parameters") {
    val op = userResource.operations.filter { op => op.method == "GET" && op.path == "/users" }.head
    val r = Play2Route(op)
    r.verb should be("GET")
    r.url should be("/users")
    r.method should be("controllers.Users.get")
    r.params.mkString(", ") should be("guid: Option[String], email: Option[String], token: Option[String]")
  }

  it("GET w/ path, guid path param, no additional parameters") {
    val op = userResource.operations.filter { op => op.method == "GET" && op.path == "/users/:guid" }.head
    val r = Play2Route(op)
    r.verb should be("GET")
    r.url should be("/users/:guid")
    r.method should be("controllers.Users.getByGuid")
    r.params.mkString(", ") should be("guid: String")
  }

  it("POST w/ default path, no parameters") {
    val op = userResource.operations.filter { op => op.method == "POST" && op.path == "/users" }.head
    val r = Play2Route(op)
    r.verb should be("POST")
    r.url should be("/users")
    r.method should be("controllers.Users.post")
    r.params.mkString(", ") should be("")
  }

  it("PUT w/ guid in path, no parameters") {
    val op = userResource.operations.filter { op => op.method == "PUT" && op.path == "/users/:guid" }.head
    val r = Play2Route(op)
    r.verb should be("PUT")
    r.url should be("/users/:guid")
    r.method should be("controllers.Users.putByGuid")
    r.params.mkString(", ") should be("guid: String")
  }

}
