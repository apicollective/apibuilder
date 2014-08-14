package core.generator

import core.{Operation, Resource, ServiceDescription}

import core.TestHelper
import org.scalatest.{ ShouldMatchers, FunSpec }

class Play2RouteGeneratorSpec extends FunSpec with ShouldMatchers {

  def getResource(service: ServiceDescription, name: String): Resource = {
    service.resources.find { _.model.name == name }.getOrElse {
      sys.error(s"Could not find $name resource")
    }
  }

  def getMethod(service: ServiceDescription, resourceName: String, method: String, path: String): Operation = {
    val resource = getResource(service, resourceName)
    resource.operations.filter { op => op.method == method && op.path == path }.headOption.getOrElse {
      val errorMsg = s"Operations found for $resourceName\n" + resource.operations.map { op =>
        "%s %s".format(op.method, op.path)
      }.mkString("\n")
      sys.error(s"Failed to find method[$method] with path[$path] for resource[${resourceName}]\n$errorMsg")
    }
  }

  describe("with apidoc service") {
    lazy val service = TestHelper.parseFile(s"api/api.json").serviceDescription.get

    describe("users resource") {
      lazy val userResource = service.resources.find { _.model.name == "user" }.getOrElse {
        sys.error("Could not find user resource")
      }

      it("GET w/ default path, parameters") {
        val op = userResource.operations.filter { op => op.method == "GET" && op.path == "/users" }.head
        val r = Play2Route(op, userResource)
        r.verb should be("GET")
        r.url should be("/users")
        r.method should be("controllers.Users.get")
        r.params.mkString(", ") should be("guid: Option[java.util.UUID], email: Option[String], token: Option[String]")
      }

      it("GET w/ path, guid path param, no additional parameters") {
        val op = userResource.operations.filter { op => op.method == "GET" && op.path == "/users/:guid" }.head
        val r = Play2Route(op, userResource)
        r.verb should be("GET")
        r.url should be("/users/:guid")
        r.method should be("controllers.Users.getByGuid")
        r.params.mkString(", ") should be("guid: java.util.UUID")
      }

      it("POST w/ default path, no parameters") {
        val op = userResource.operations.filter { op => op.method == "POST" && op.path == "/users" }.head
        val r = Play2Route(op, userResource)
        r.verb should be("POST")
        r.url should be("/users")
        r.method should be("controllers.Users.post")
        r.params.mkString(", ") should be("")
      }

      it("PUT w/ guid in path, no parameters") {
        val op = userResource.operations.filter { op => op.method == "PUT" && op.path == "/users/:guid" }.head
        val r = Play2Route(op, userResource)
        r.verb should be("PUT")
        r.url should be("/users/:guid")
        r.method should be("controllers.Users.putByGuid")
        r.params.mkString(", ") should be("guid: java.util.UUID")
      }
    }

    describe("membership_request resource") {
      lazy val membershipRequestResource = service.resources.find { _.model.name == "membership_request" }.getOrElse {
        sys.error("Could not find membership_request resource")
      }

      it("POST /membership_requests/:guid/accept") {
        val op = membershipRequestResource.operations.filter { op => op.method == "POST" && op.path == "/membership_requests/:guid/accept" }.head
        val r = Play2Route(op, membershipRequestResource)
        r.verb should be("POST")
        r.url should be("/membership_requests/:guid/accept")
        r.method should be("controllers.MembershipRequests.postAcceptByGuid")
        r.params.mkString(", ") should be("guid: java.util.UUID")
      }
    }

    describe("service resource") {
      it("GET /:orgKey") {
        val membershipRequestResource = getResource(service, "membership_request")
        val op = getMethod(service, "service", "GET", "/:orgKey")
        val r = Play2Route(op, membershipRequestResource)
        r.method should be("controllers.Services.getByOrgKey")
      }
    }
  }

  describe("with reference-api service") {
    lazy val service = TestHelper.parseFile(s"reference-api/api.json").serviceDescription.get

    it("supports multiple query parameters") {
      val echoResource = getResource(service, "echo")
      val op = getMethod(service, "echo", "GET", "/echoes")
      val r = Play2Route(op, echoResource)
      r.method should be("controllers.Echoes.get")
      r.params.mkString(" ") should be("foo: Option[String]")
      r.paramComments.getOrElse("") should be("""
# Additional parameters to GET /echos
#   - optional_messages: Option[Seq[String]]
#   - required_messages: Seq[String]
""".trim)

      TestHelper.assertEqualsFile(
        "core/src/test/resources/generators/play-2-route-reference-api.routes",
        Play2RouteGenerator(service).generate().getOrElse("")
      )
    }

    it("camel cases hypen in route") {
      val echoResource = getResource(service, "echo")
      val op = getMethod(service, "echo", "GET", "/echoes/arrays-only")
      val r = Play2Route(op, echoResource)
      r.method should be("controllers.Echoes.getArraysOnly")
    }

  }

}
