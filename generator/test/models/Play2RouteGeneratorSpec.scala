package models

import lib.Primitives
import com.gilt.apidocspec.models.{Container, Operation, Resource, Service, Type, TypeKind, TypeInstance}
import generator.ScalaService
import org.scalatest.{ ShouldMatchers, FunSpec }

class Play2RouteGeneratorSpec extends FunSpec with ShouldMatchers {

  def getResource(service: Service, name: String): Resource = {
    service.resources.find { _.model.name == name }.getOrElse {
      sys.error(s"Could not find $name resource")
    }
  }

  def getMethod(service: Service, resourceName: String, method: String, path: String): Operation = {
    val resource = getResource(service, resourceName)
    resource.operations.filter { op => op.method == method && op.path == path }.headOption.getOrElse {
      val errorMsg = s"Operations found for $resourceName\n" + resource.operations.map { op =>
        "%s %s".format(op.method, op.path)
      }.mkString("\n")
      sys.error(s"Failed to find method[$method] with path[$path] for resource[${resourceName}]\n$errorMsg")
    }
  }

  describe("with apidoc service") {
    lazy val service = TestHelper.parseFile(s"../api/api.json").serviceDescription.get
    lazy val ssd = new ScalaService(service)

    describe("users resource") {
      lazy val userResource = service.resources.find { _.model.name == "user" }.getOrElse {
        sys.error("Could not find user resource")
      }

      it("GET w/ default path, parameters") {
        val op = userResource.operations.filter { op => op.method == "GET" && op.path == "/users" }.head
        val r = Play2Route(ssd, op, userResource)
        r.verb should be("GET")
        r.url should be("/users")
        r.method should be("controllers.Users.get")
        r.params.mkString(", ") should be("guid: scala.Option[_root_.java.util.UUID], email: scala.Option[String], token: scala.Option[String]")
      }

      it("GET w/ path, guid path param, no additional parameters") {
        val op = userResource.operations.filter { op => op.method == "GET" && op.path == "/users/:guid" }.head
        val r = Play2Route(ssd, op, userResource)
        r.verb should be("GET")
        r.url should be("/users/:guid")
        r.method should be("controllers.Users.getByGuid")
        r.params.mkString(", ") should be("guid: _root_.java.util.UUID")
      }

      it("POST w/ default path, no parameters") {
        val op = userResource.operations.filter { op => op.method == "POST" && op.path == "/users" }.head
        val r = Play2Route(ssd, op, userResource)
        r.verb should be("POST")
        r.url should be("/users")
        r.method should be("controllers.Users.post")
        r.params.mkString(", ") should be("")
      }

      it("PUT w/ guid in path, no parameters") {
        val op = userResource.operations.filter { op => op.method == "PUT" && op.path == "/users/:guid" }.head
        val r = Play2Route(ssd, op, userResource)
        r.verb should be("PUT")
        r.url should be("/users/:guid")
        r.method should be("controllers.Users.putByGuid")
        r.params.mkString(", ") should be("guid: _root_.java.util.UUID")
      }
    }

    describe("membership_request resource") {
      lazy val membershipRequestResource = service.resources.find { _.model.name == "membership_request" }.getOrElse {
        sys.error("Could not find membership_request resource")
      }

      it("POST /membership_requests/:guid/accept") {
        val op = membershipRequestResource.operations.filter { op => op.method == "POST" && op.path == "/membership_requests/:guid/accept" }.head
        val r = Play2Route(ssd, op, membershipRequestResource)
        r.verb should be("POST")
        r.url should be("/membership_requests/:guid/accept")
        r.method should be("controllers.MembershipRequests.postAcceptByGuid")
        r.params.mkString(", ") should be("guid: _root_.java.util.UUID")
      }
    }

    describe("service resource") {
      it("GET /:orgKey") {
        val membershipRequestResource = getResource(service, "membership_request")
        val op = getMethod(service, "service", "GET", "/:orgKey")
        val r = Play2Route(ssd, op, membershipRequestResource)
        r.method should be("controllers.Services.getByOrgKey")
      }
    }
  }

  describe("with reference-api service") {
    lazy val service = TestHelper.parseFile(s"reference-api/api.json").serviceDescription.get
    lazy val ssd = new ScalaService(service)

    it("normalizes explicit paths that match resource name") {
      val resource = getResource(service, "organization")
      val op = getMethod(service, "organization", "GET", "/organizations")
      val r = Play2Route(ssd, op, resource)
      r.method should be("controllers.Organizations.get")
    }

    it("enums are strongly typed") {
      val resource = getResource(service, "user")
      val op = getMethod(service, "user", "GET", "/users/:age_group")
      val r = Play2Route(ssd, op, resource)
      r.method should be("controllers.Users.getByAgeGroup")
      r.params.mkString("") should be("age_group: apidocreferenceapi.models.AgeGroup")
    }

    it("supports multiple query parameters") {
      val echoResource = getResource(service, "echo")
      val op = getMethod(service, "echo", "GET", "/echoes")
      val r = Play2Route(ssd, op, echoResource)
      r.method should be("controllers.Echoes.get")
      r.params.mkString(" ") should be("foo: scala.Option[String]")
      r.paramComments.getOrElse("") should be("""
# Additional parameters to GET /echoes
#   - optional_messages: scala.Option[Seq[String]]
#   - required_messages: Seq[String]
""".trim)

      TestHelper.assertEqualsFile(
        "test/resources/generators/play-2-route-reference-api.routes",
        Play2RouteGenerator(service).generate().getOrElse("")
      )
    }

    it("camel cases hypen in route") {
      val echoResource = getResource(service, "echo")
      val op = getMethod(service, "echo", "GET", "/echoes/arrays-only")
      val r = Play2Route(ssd, op, echoResource)
      r.method should be("controllers.Echoes.getArraysOnly")
    }

  }

  describe("with quality service example") {

    lazy val quality = TestHelper.parseFile("test/resources/examples/quality.json").serviceDescription.get

    it("correctly orders parameters defined in path and parameters") {
      val op = getMethod(quality, "agenda_item", "DELETE", "/meetings/:meeting_id/agenda_items/:id")
      op.parameters.map(_.name) should be(Seq("meeting_id", "id"))
      op.parameters.head.`type` should be(TypeInstance(Container.Singleton, Type(TypeKind.Primitive, Primitives.Long.toString)))
    }

  }

}
